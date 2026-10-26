import os,sys,socket,subprocess,time,hashlib,uuid,argparse,shutil,ctypes,json
from pathlib import Path
repo=Path(__file__).resolve().parent.parent;root=repo.parent;tools=Path(os.environ.get('CRIMEANDLAW_BUILD_TOOLS',str(root/'build-tools')))
arg=argparse.ArgumentParser();arg.add_argument('--visual',action='store_true');arg.add_argument('--output',default='tcp-1');args=arg.parse_args();out=repo/'build/garden-smoke'/args.output
if out.exists() and any(out.iterdir()):raise SystemExit('Choose a fresh --output directory; previous results must not be reused.')
out.mkdir(parents=True,exist_ok=True)
env=os.environ.copy()
for key in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','GRADLE_OPTS']:env.pop(key,None)
env['MOD_CLASSES']=':'.join('crimeandlaw%%'+str(p) for p in [repo/'build/classes/java/main',repo/'build/resources/main'])
env.update(LD_LIBRARY_PATH=str(tools/'x11/root/usr/lib/x86_64-linux-gnu'),DISPLAY='127.0.0.1:78',LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
runtime=out/'runtime';runtime.mkdir(mode=0o700,exist_ok=True);env['XDG_RUNTIME_DIR']=str(runtime)
procs=[];logs=[]
def launch(role,restart=False):
 server=role=='server';game=out/('game-'+role);game.mkdir(exist_ok=True)
 if server:
  (game/'eula.txt').write_text('eula=true\n');(game/'server.properties').write_text('online-mode=false\nserver-port=25598\nserver-ip=127.0.0.1\nlevel-type=minecraft:flat\nspawn-protection=0\nview-distance=5\nsimulation-distance=2\ndifficulty=peaceful\nmax-tick-time=0\n')
 name='server' if server else 'client';cp=[str(repo/'build/moddev/artifacts/neoforge-21.1.248.jar')]+(repo/f'build/moddev/{name}LegacyClasspath.txt').read_text().splitlines()+[str(repo/'build/classes/java/main'),str(repo/'build/resources/main')]
 native=out/('natives-'+role);native.mkdir(exist_ok=True)
 command=[str(tools/'jdk-21.0.2/bin/java'),'-Xmx1400m','-Dorg.lwjgl.system.SharedLibraryExtractPath='+str(native),'-Dcrimeandlaw.gardenOutput='+str(out)]
 if server:command+=['-Dcrimeandlaw.gardenSmoke=true','-Dcrimeandlaw.gardenRestart='+str(restart).lower()]
 else:command+=['-Dcrimeandlaw.gardenRole='+role]
 command+=['-cp',':'.join(cp),'@'+str(repo/f'build/moddev/{name}RunVmArgs.txt'),'@'+str(repo/f'build/moddev/{name}RunProgramArgs.txt')]
 if server:command+=['--nogui']
 else:
  user='GardenOwner' if role=='owner' else 'GardenRival' if role=='rival' else 'GardenVisual';d=bytearray(hashlib.md5(('OfflinePlayer:'+user).encode()).digest());d[6]=(d[6]&15)|48;d[8]=(d[8]&63)|128;command+=['--username',user,'--uuid',str(uuid.UUID(bytes=bytes(d))),'--width','1920','--height','1080']
 log=(out/(role+('-restart' if restart else '')+'.log')).open('w');logs.append(log);p=subprocess.Popen(command,cwd=game,env=env,stdout=log,stderr=subprocess.STDOUT);procs.append(p);return p
try:
 log=(out/'xvfb.log').open('w');logs.append(log);xv=subprocess.Popen([str(tools/'x11/root/usr/bin/Xvfb'),':78','-screen','0','1920x1080x24','-ac','-nolisten','unix','-nolisten','local','-listen','tcp','-xkbdir','/usr/share/X11/xkb'],env=env,stdout=log,stderr=subprocess.STDOUT);procs.append(xv)
 for _ in range(60):
  if xv.poll() is not None:raise RuntimeError('Xvfb failed')
  try:
   with socket.create_connection(('127.0.0.1',6078),timeout=.2):break
  except OSError:time.sleep(.1)
 x11=ctypes.CDLL('libX11.so.6');x11.XOpenDisplay.argtypes=[ctypes.c_char_p];x11.XOpenDisplay.restype=ctypes.c_void_p;x11.XCloseDisplay.argtypes=[ctypes.c_void_p]
 for _ in range(60):
  if xv.poll() is not None:raise RuntimeError('Xvfb initialization failed; inspect xvfb.log')
  display=x11.XOpenDisplay(env['DISPLAY'].encode())
  if display:x11.XCloseDisplay(display);break
  time.sleep(.1)
 else:raise RuntimeError('X display initialization timeout')
 if args.visual:
  p=launch('visual');p.wait(timeout=180)
  if not (out/'visual.PASS').exists():raise RuntimeError('Visual validation failed')
  print('PASS visual gallery',flush=True)
 else:
  p=launch('server');deadline=time.monotonic()+100
  while True:
   if p.poll() is not None:raise RuntimeError('Server startup failed')
   try:
    with socket.create_connection(('127.0.0.1',25598),timeout=.3):break
   except OSError:time.sleep(.2)
   if time.monotonic()>deadline:raise RuntimeError('Server listen timeout')
  clients={role:launch(role) for role in ['rival','owner']};deadline=time.monotonic()+500
  while p.poll() is None:
   for role,process in clients.items():
    if process.poll() is not None and not (out/(role+'.PASS')).exists():raise RuntimeError(role+' client exited before verification; inspect its log')
   failed=list(out.glob('*.FAIL'))
   if failed:raise RuntimeError('\n'.join(f.name+': '+f.read_text() for f in failed))
   if time.monotonic()>deadline:raise RuntimeError('Network validation timeout')
   time.sleep(.3)
  (out/'first-server-exit.json').write_text(json.dumps({'server_exit':p.returncode,'clients':{role:process.poll() for role,process in clients.items()}}))
  if p.returncode!=0:raise RuntimeError('First server exited abnormally: '+str(p.returncode))
  if not all((out/(n+'.PASS')).exists() for n in ['owner','rival','server','security']):raise RuntimeError('Missing network PASS')
  shutil.copytree(out/'game-server/world',out/'before-restart-world')
  p=launch('server',True);p.wait(timeout=100)
  if not (out/'restart.PASS').exists():raise RuntimeError('Disk restart failed')
  print('PASS two TCP clients, adversarial checks and disk restart',flush=True)
finally:
 for p in reversed(procs):
  if p.poll() is None:
   p.terminate()
   try:p.wait(timeout=10)
   except subprocess.TimeoutExpired:p.kill();p.wait()
 for log in logs:log.close()
