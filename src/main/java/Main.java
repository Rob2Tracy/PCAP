import io.pkts.Pcap;
import java.io.IOException;
import java.util.Scanner;


public class Main {


    public static void main(String[] args){

        Menu menu = new Menu();

        Boolean menuCheck = menu.preCheck(args); // Launches menu

        menu.menu(menuCheck);





    }


}
