# COC 2 — Lesson 2: Network Installation and Configuration

## Definition of Terms

Lesson 1 — Definition of Terms

Study these key terms before starting the lesson.

1.1 Understand the network before installing

A network allows computers and other devices to exchange data and share resources. Before installation, identify the purpose of the network, the number and location of devices, the available internet connection, the required coverage, and the safest cable route.

Figure 1. Example SOHO layout. The router connects the local network to the internet; the switch expands the number of wired connections; an access point provides Wi-Fi.

1.2 Common network components

Modem/ONT: terminates the service-provider connection. Some units combine modem/ONT and router functions.

Figure 1.1. An image of a modem.

A modem or ONT usually looks like a small rectangular electronic box with indicator lights and ports for connecting cables.

Router: connects the LAN to other networks and commonly provides DHCP, NAT, firewall, and Wi-Fi functions.

Figure 1.2. An image of a router.

A router is the device that connects your local network (LAN) to other networks, usually the Internet. It directs data between devices and commonly provides DHCP, NAT, firewall, and Wi-Fi functions.

Switch: connects multiple wired devices in one LAN.

Figure 1.3.  An image of what looks like a switch.

A network switch is a device that connects multiple wired devices within the same Local Area Network (LAN). Think of it as an expansion device that gives your network more Ethernet ports.

Wireless access point: provides Wi-Fi access to the LAN. A router may already contain an access point.

Figure 1.4. An image of a wireless access point.

A wireless access point (WAP) is a networking device that allows wireless devices, such as laptops, smartphones, and tablets, to connect to a wired Local Area Network (LAN) through Wi-Fi.

Think of it as a device that adds Wi-Fi connectivity to an existing wired network.

Note: A wireless access point (WAP) and a router are different networking devices, although they can look very similar. They can even look identical because modern networking equipment often combines multiple functions into one device.

NIC: the Ethernet or Wi-Fi interface installed in a computer or device.

Figure 1.5. An image of NIC.

An NIC (Network Interface Card) is a hardware component that allows a computer or device to connect to a network.

Think of it as the computer's communication equipment. Without an NIC, a computer cannot communicate with other devices over a network.

Ethernet cable: carries network data between wired devices.

Figure 1.6. An image of ethernet cable.

An Ethernet cable is a physical network cable used to connect devices, such as computers, routers, and switches, so they can communicate and exchange data over a network.

Think of it as a road that carries information between network devices.

1.3 Materials, tools, and safety

Typical materials include Cat5e or Cat6 UTP cable, RJ-45 plugs compatible with the cable, keystone jacks or wall plates, cable clips/raceway.

Typical tools include a cable cutter/stripper, RJ-45 crimping tool, punch-down tool (for keystone jacks), cable tester.

Safety checklist: obtain permission; inspect the work area; avoid wet locations and trip hazards; do not run data cable where it may be crushed or sharply bent; keep appropriate separation from power wiring; use tools correctly; secure ladders and wear eye protection when needed; follow housekeeping practices and dispose of offcuts properly.

1.4 Ethernet cable and T568B termination

Twisted-pair Ethernet cable contains four pairs of insulated copper conductors. The twists help reduce interference. For a common straight-through patch cable, both ends use the same wiring standard. T568B is widely used; follow the standard specified by the site or instructor.

Figure 1.13. T568B order, pins 1–8: White/Orange, Orange, White/Green, Blue, White/Blue, Green, White/Brown, Brown.

Figure 1.14. Wiring diagram

Lesson 2 — Definition of Terms

Study these key terms before starting the lesson.

2.1 IPv4 settings

For devices to communicate on an IP network, they need compatible network settings. The key IPv4 settings are IP address, subnet mask, default gateway, and DNS server.

Example network: Router LAN address 192.168.1.1; subnet mask 255.255.255.0; PC 1 address 192.168.1.10; PC 2 address 192.168.1.11. These addresses are examples only. Check the actual network before assigning addresses and avoid conflicts.

2.2 DHCP versus static addressing

DHCP automatically leases IP settings to clients and is suitable for most ordinary workstations. Static addressing manually assigns a fixed address and is often used for infrastructure devices or equipment that must keep a known address. A manually assigned address must not conflict with another device or the DHCP pool.

2.3 Configure and verify a Windows client

Connect the computer to the network using Ethernet or Wi-Fi.

Figure 2.0.

Open the network adapter settings and choose automatic (DHCP) or enter the approved static values.

Figure 2.1.

Open Command Prompt and run ipconfig /all to inspect the adapter's settings.

Figure 2.2.

Run ping 127.0.0.1 to test the local TCP/IP loopback, then ping the computer's own address.

Figure 2.3.

Ping the default gateway, then another known device on the same LAN.

Figure 2.4.

If authorized and internet access is expected, test a known external IP and then a domain name to help distinguish connectivity from DNS problems.

Figure 2.5.

Example commands (Windows):

ipconfig /all
ping 192.168.1.1
ping 192.168.1.11
nslookup example.com

2.4 Basic troubleshooting

No link light: check cable seating, cable damage, NIC status, and switch/router power.

169.254.x.x address: the client may not have received a DHCP lease; check the cable/Wi-Fi, DHCP service, and router.

Can ping gateway but not a domain name: check DNS settings and DNS reachability.

Duplicate IP warning: identify the conflicting device and correct the addressing plan.

One PC cannot communicate: compare its IP settings with a working PC and check adapter status, VLAN/firewall rules, and cabling.

Lesson 3 — Definition of Terms

Study these key terms before starting the lesson.

3.1 Access the router safely

Use the router's documented management address and authorized administrator credentials. Change default administrator credentials, update firmware using the manufacturer's instructions, and save a backup of the current configuration before making significant changes. Do not factory-reset a shared router without permission.

3.2 Configure basic wireless settings

Set a clear SSID that does not reveal unnecessary personal information.

Figure 3.0.

Choose WPA3-Personal when all required devices support it; otherwise use WPA2-Personal with AES. Avoid WEP, obsolete TKIP-only modes, and open security.

Figure 3.1.

Use a long, unique Wi-Fi passphrase and do not reuse the router administrator password.

Figure 3.2.

Disable features such as WPS or remote administration if not needed and supported policy recommends disabling them.

Figure 3.3.

3.3 LAN, WAN, firewall, and access controls

The WAN interface connects toward the internet service provider. The LAN interface serves local devices. Configure WAN settings only as supplied by the ISP or network administrator. Keep the firewall enabled. MAC filtering can be used as a basic administrative control, but it is not strong authentication because MAC addresses can be copied or spoofed. Prefer WPA2/WPA3 security and appropriate network access policies.
