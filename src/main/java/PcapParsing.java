import io.pkts.Pcap;
import io.pkts.framer.EthernetFramer;
import io.pkts.packet.*;
import io.pkts.protocol.Protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PcapParsing {

    private Pcap pcap;
    private TCPPacket tcpPacket;
    private IPv4Packet iPv4Packet;
    private HttpGetModel getModel;
    private ArrayList<HttpGetModel> completeGetList;
    private int packetSplitLength,destPort,sourcePort,check,headerLength;
    private String currentLine,currentBuffer,getRequest,destIp,sourceIp,webHost,userAgent,accept,origin,firstOctet,secondOctet,thirdOctet,fourthOctet,sourceMAC, destMAC;
    private ArrayList<TCPPacketModel> tcpPackets;
    private TCPPacketModel tcpPacketModel;
    private long ackNumber,seqNumber,arrivalTime;
    private ArrayList<String> flagsSet;
    private Set<String> uniqueSourceIps, uniqueDestIps, uniqueWebhost,uniqueSourceMACs;
    private ArpModel arpModel;
    private ArrayList<ArpModel> arpList;






    public PcapParsing(Pcap pcap) {
        this.pcap = pcap;
        uniqueDestIps = new HashSet<>();
        uniqueSourceIps = new HashSet<>();
        uniqueWebhost = new HashSet<>();
    }


    public ArrayList<TCPPacketModel> tcpPacketParsing()
    {
        tcpPackets = new ArrayList<>();
        flagsSet = new ArrayList<>();

        try {
            pcap.loop((final Packet packet) -> {


                if (packet.hasProtocol(Protocol.TCP)) {

                    tcpPacket = (TCPPacket) packet.getPacket(Protocol.TCP);  // Grabs information from packet
                    iPv4Packet = (IPv4Packet) packet.getPacket(Protocol.IPv4);

                    destIp = iPv4Packet.getDestinationIP();
                    sourceIp = iPv4Packet.getSourceIP();

                    destPort = tcpPacket.getDestinationPort();
                    sourcePort = tcpPacket.getSourcePort();

                    ackNumber = tcpPacket.getAcknowledgementNumber();
                    currentBuffer = String.valueOf(tcpPacket.getPayload());

                    seqNumber = tcpPacket.getSequenceNumber();
                    arrivalTime = tcpPacket.getArrivalTime();

                    headerLength = tcpPacket.getHeaderLength();


                    if(tcpPacket.isSYN()) // Determines what flags are set int he packet since TCP
                    {
                        flagsSet.add("SYN");

                    }
                    if(tcpPacket.isACK())
                    {
                        flagsSet.add("ACK");
                    }
                    if(tcpPacket.isCWR())
                    {
                        flagsSet.add("CWR");
                    }
                    if(tcpPacket.isECE())
                    {
                        flagsSet.add("ECE");
                    }
                    if(tcpPacket.isFIN())
                    {
                        flagsSet.add("FIN");
                    }
                    if(tcpPacket.isPSH())
                    {
                        flagsSet.add("PSH");
                    }
                    if(tcpPacket.isRST())
                    {
                        flagsSet.add("RST");
                    }
                    if(tcpPacket.isURG())
                    {
                        flagsSet.add("URG");
                    }




                }

                tcpPacketModel = new TCPPacketModel(sourceIp,destIp,currentBuffer,sourcePort,destPort,ackNumber,seqNumber,flagsSet,arrivalTime,headerLength);
                tcpPackets.add(tcpPacketModel); // Adds new filed model to tcp packet list

                uniqueSourceIps.add(sourceIp); // Adds ips to a Set which only allow unique elements
                uniqueDestIps.add(destIp);

                return true;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tcpPackets;
    }




    public ArrayList<HttpGetModel> getRequestParsing(ArrayList<TCPPacketModel> tcpPacketList)
    {
        webHost = "NOWEBHOST"; //Sets default values since fields can not exist / be empty
        origin = "NOORIGIN";
        accept = "NOACCEPT";
        userAgent = "NOUSERAGENT";
        currentBuffer = "NOPAYLOAD";
        check = 0;

        completeGetList = new ArrayList<>();


                 for(int i=0; i < tcpPacketList.size(); i++) {

                     destPort = tcpPacketList.get(i).getDestPort();
                     currentBuffer = tcpPacketList.get(i).getPayload();

                     if (destPort == 80 || destPort == 8080) { // Look for HTTP traffic going to web server

                         sourcePort = tcpPacketList.get(i).getSourcePort();

                         sourceIp = tcpPacketList.get(i).getSourceIP();
                         destIp = tcpPacketList.get(i).getDestIP();

                         packetSplitLength = currentBuffer.split("\n").length;

                         for (int y = 0; y < packetSplitLength; y++) {
                             currentLine = currentBuffer.split("\n")[y];
                             if (currentLine.contains("GET")) { // Can simply look at packet payload for GET request since unencrypted

                                 getRequest = currentLine.split("GET")[1];
                                 check = 1; // If GET request is found sets counter and allows it to be added to the list
                             }

                                 else if (currentLine.contains("Host")) { //Each row in the packet is labeled. Can look for label and then the start of the value to grab value

                                     webHost = currentLine.substring(6, currentLine.length() - 1);


                                 } else if (currentLine.contains("User-Agent")) {

                                     userAgent = currentLine.substring(12, currentLine.length() - 1);

                                 } else if (currentLine.contains("Accept")) {
                                     accept = currentLine.substring(9, currentLine.length() - 1);

                                 } else if (currentLine.contains("Origin")) {
                                     origin = currentLine.substring(8, currentLine.length() - 1);
                                 }


                                 if(check == 1) { // If packet is a GET request
                                     check = 0;
                                     getModel = new HttpGetModel(getRequest, accept, userAgent, webHost, sourceIp, sourcePort, destIp, destPort, origin);

                                     completeGetList.add(getModel);


                                     if (!webHost.contains("NOWEBHOST")) {
                                         uniqueWebhost.add(webHost);
                                     }
                                 }

                     }
                         if(currentBuffer.contains("exe")||currentBuffer.contains("zip")||currentBuffer.contains("tar")||currentBuffer.contains("msi")) // Need to add arraylist of file extensions it checks
                         {
                             System.out.println("#########################################");
                             System.out.println("Download found");
                             System.out.println("Source IP: " + sourceIp);
                             System.out.println("Destination IP: " + destIp);
                             System.out.println("Webhost/URL: " + webHost);

                             System.out.println("#########################################");
                         }

                     }


                 }



        return completeGetList;
    }

    public ArrayList<ArpModel> arpParsing(Pcap ppcap)
    {
        arpList = new ArrayList<>();
        uniqueSourceMACs = new HashSet<>();
        uniqueSourceIps.clear();

        System.out.println("Starting parsing for ARP packets");

        try {
            ppcap.loop((final Packet packet) -> {
                PCapPacket macPacket = (PCapPacket) packet.getPacket(Protocol.PCAP);  // Loop through pcap
               EthernetFramer framer = new EthernetFramer();


              MACPacket impl =  framer.frame(macPacket,packet.getPayload());

                firstOctet = String.valueOf(hexToInt(gARP(38,packet.getPayload().getArray())));   // Since Graditous ARP is a fixed packet format we can
                secondOctet = String.valueOf(hexToInt(gARP(39,packet.getPayload().getArray())));  // hardcode where to look for values in the packet
                thirdOctet = String.valueOf(hexToInt(gARP(40,packet.getPayload().getArray())));
                fourthOctet = String.valueOf(hexToInt(gARP(41,packet.getPayload().getArray())));

                destIp = firstOctet +"."+ secondOctet +"." + thirdOctet + "." +fourthOctet;

                firstOctet = String.valueOf(hexToInt(gARP(28,packet.getPayload().getArray())));
                secondOctet = String.valueOf(hexToInt(gARP(29,packet.getPayload().getArray())));
                thirdOctet = String.valueOf(hexToInt(gARP(30,packet.getPayload().getArray())));
                fourthOctet = String.valueOf(hexToInt(gARP(31,packet.getPayload().getArray())));

                sourceIp = firstOctet +"."+ secondOctet +"." + thirdOctet + "." +fourthOctet;

                sourceMAC = impl.getSourceMacAddress();
                destMAC = impl.getDestinationMacAddress();
                arrivalTime = impl.getArrivalTime();

                arpModel = new ArpModel(destIp,sourceIp,destMAC,sourceMAC,arrivalTime); // Packages findings into object for easy access

                arpList.add(arpModel);

                uniqueSourceMACs.add(sourceMAC); // For analysis function



                return true;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Done parsing for ARP packets");

        return arpList;
    }

    public String gARP(int index,byte[] byteArray)
    {
        byte temp = byteArray[index]; // turns byte into hex

        return String.format("%02X",temp);

    }

    public int hexToInt(String hex)
    {
     return Integer.parseInt(hex,16);
    } // turns hex into int for ip address octet


    public Set<String> getUniqueSourceIps() {
        return uniqueSourceIps;
    } // For later stuff

    public Set<String> getUniqueDestIps() {
        return uniqueDestIps;
    }

    public Set<String> getUniqueWebhost() {
        return uniqueWebhost;
    }

    public Set<String> getUniqueSourceMACs() {
        return uniqueSourceMACs;
    }
}
