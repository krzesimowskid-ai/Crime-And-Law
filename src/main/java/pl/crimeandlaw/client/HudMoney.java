package pl.crimeandlaw.client;

import pl.crimeandlaw.network.GangNetwork;

final class HudMoney {
    static long cash,bank;
    static boolean ready;
    private HudMoney(){}
    static void receive(GangNetwork.Money money){cash=money.cash();bank=money.bank();ready=true;}
    static void clear(){cash=bank=0;ready=false;}
}
