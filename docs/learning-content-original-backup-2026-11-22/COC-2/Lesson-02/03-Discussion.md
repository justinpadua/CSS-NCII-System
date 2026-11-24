COC 2 — LESSON 2: Discussion
2.1 IPv4 settings
For devices to communicate on an IP network, they need compatible network settings. The key IPv4 settings are IP address, subnet mask, default gateway, and DNS server.
Example network: Router LAN address 192.168.1.1; subnet mask 255.255.255.0; PC 1 address 192.168.1.10; PC 2 address 192.168.1.11. These addresses are examples only. Check the actual network before assigning addresses and avoid conflicts.
Setting	Example	Purpose
IP address	192.168.1.10	Identifies the computer on the network.
Subnet mask	255.255.255.0	Defines the local IPv4 network range.
Default gateway	192.168.1.1	Sends traffic to other networks.
DNS server	192.168.1.1 or approved DNS	Resolves domain names to IP addresses.
2.2 DHCP versus static addressing
DHCP automatically leases IP settings to clients and is suitable for most ordinary workstations. Static addressing manually assigns a fixed address and is often used for infrastructure devices or equipment that must keep a known address. A manually assigned address must not conflict with another device or the DHCP pool.
2.3 Configure and verify a Windows client
1.	Connect the computer to the network using Ethernet or Wi-Fi.

 
Figure 2.0.

2.	Open the network adapter settings and choose automatic (DHCP) or enter the approved static values.

 
Figure 2.1.
3.	Open Command Prompt and run ipconfig /all to inspect the adapter's settings.

 
Figure 2.2. 

4.	Run ping 127.0.0.1 to test the local TCP/IP loopback, then ping the computer's own address.

 
Figure 2.3.

5.	Ping the default gateway, then another known device on the same LAN.

 
Figure 2.4.









6.	If authorized and internet access is expected, test a known external IP and then a domain name to help distinguish connectivity from DNS problems.
 
Figure 2.5.

Example commands (Windows):
ipconfig /all
ping 192.168.1.1
ping 192.168.1.11
nslookup example.com
2.4 Basic troubleshooting
•	No link light: check cable seating, cable damage, NIC status, and switch/router power.
•	169.254.x.x address: the client may not have received a DHCP lease; check the cable/Wi-Fi, DHCP service, and router.
•	Can ping gateway but not a domain name: check DNS settings and DNS reachability.
•	Duplicate IP warning: identify the conflicting device and correct the addressing plan.
•	One PC cannot communicate: compare its IP settings with a working PC and check adapter status, VLAN/firewall rules, and cabling.
