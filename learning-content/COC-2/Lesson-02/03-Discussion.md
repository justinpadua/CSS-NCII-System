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

2.	Open the network adapter settings and choose automatic (DHCP) or enter the approved static values.
3.	Open Command Prompt and run ipconfig /all to inspect the adapter's settings.

4.	Run ping 127.0.0.1 to test the local TCP/IP loopback, then ping the computer's own address.

5.	Ping the default gateway, then another known device on the same LAN.









6.	If authorized and internet access is expected, test a known external IP and then a domain name to help distinguish connectivity from DNS problems.

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

### 2.5 Network equipment and Wi-Fi configuration

A router forwards traffic between networks and commonly provides gateway, DHCP, firewall, and sometimes wireless-access functions. A separate switch connects wired devices within a local network; an access point provides Wi-Fi access. Consumer gateways may combine several functions in one device. Follow the network plan and equipment manual rather than assuming every device has the same address or features.

For authorized router administration, use the documented management address and approved credentials. Back up the current configuration before significant changes. Do not factory-reset a shared router without permission. Change default administrator credentials, keep firmware current using vendor instructions, and keep the firewall enabled.

Set an SSID that does not expose unnecessary personal information. Prefer WPA3-Personal when required devices support it; otherwise use WPA2-Personal with AES. Avoid WEP, obsolete TKIP-only modes, and open Wi-Fi. Use a long, unique Wi-Fi passphrase separate from the router administrator password. Disable WPS or remote administration when not needed and when consistent with site policy. MAC filtering is not strong authentication because addresses can be copied or spoofed.

The WAN interface connects toward the Internet service provider; LAN interfaces serve local devices. Configure WAN settings only from approved ISP or administrator instructions. Test a client against the approved network plan and do not put real passwords in screenshots or student reports.

