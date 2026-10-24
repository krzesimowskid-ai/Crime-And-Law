package pl.crimeandlaw.client;

/** Reserve the chat column before placing the capture panel in native GUI coordinates. */
public record CaptureHudLayout(int x,int y,int width,int height) {
    public static CaptureHudLayout of(int screenWidth,ChatLayout chat){
        int margin=Math.min(10,Math.max(0,screenWidth/20));
        int width=Math.min(280,Math.max(1,screenWidth-margin*2));
        int x=(screenWidth-width)/2;
        if(chat!=null){
            int chatRight=(int)Math.ceil((chat.x()+chat.width()+10)*chat.scale());
            x=Math.max(x,chatRight+6);
            width=Math.min(width,Math.max(1,screenWidth-margin-x));
        }
        return new CaptureHudLayout(x,12,width,40);
    }
}
