COC 2 — LESSON 3: Discussion
3.1 Access the router safely
Use the router's documented management address and authorized administrator credentials. Change default administrator credentials, update firmware using the manufacturer's instructions, and save a backup of the current configuration before making significant changes. Do not factory-reset a shared router without permission.
3.2 Configure basic wireless settings
• Set a clear SSID that does not reveal unnecessary personal information.

Figure 3.0.

• Choose WPA3-Personal when all required devices support it; otherwise use WPA2-Personal with AES. Avoid WEP, obsolete TKIP-only modes, and open security.

Figure 3.1.

• Use a long, unique Wi-Fi passphrase and do not reuse the router administrator password.

Figure 3.2.

• Disable features such as WPS or remote administration if not needed and supported policy recommends disabling them.

Figure 3.3.

3.3 LAN, WAN, firewall, and access controls
The WAN interface connects toward the internet service provider. The LAN interface serves local devices. Configure WAN settings only as supplied by the ISP or network administrator. Keep the firewall enabled. MAC filtering can be used as a basic administrative control, but it is not strong authentication because MAC addresses can be copied or spoofed. Prefer WPA2/WPA3 security and appropriate network access policies.
Example: small office Wi-Fi setup
A small office has a router at 192.168.1.1, DHCP enabled for ordinary client devices, and an SSID named Office-Lab. The administrator selects WPA2-AES or WPA3, creates a strong unique passphrase, and tests a laptop's connection. The laptop should receive an address in the planned subnet, reach the gateway, and resolve domain names if internet/DNS service is available.
