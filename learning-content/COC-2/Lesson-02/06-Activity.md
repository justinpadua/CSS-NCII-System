# COC 2 — Lesson 2: Network Installation and Configuration

## Activity

**Learning objective:** Apply the existing cable task, identify IPv4/router settings, plan safe client verification, and select secure wireless practices.

**Materials:** Worksheet and pen. Optional practical tasks require instructor-approved patch cable, compatible 8P8C plugs, cable stripper/cutter, crimping tool, cable tester, lab switch/router/access point, one lab client, and an authorized lab network plan. Sample output cards/screenshots may replace equipment. Follow tool instructions, protect eyes from offcuts, and keep the work area clear. No paid tools or public/shared-router changes are required.

Lesson 1 — Activity: Cable It Right

Practice identifying network parts, wiring a T568B cable, and working safely.

How to use: click each gray field in Microsoft Word to pick an answer from the drop-down list, tick a box, or type your response.

Part A – Match the Component

Choose the component that fits each description.  Word bank: Ethernet cable • Keystone jack • Modem / ONT • NIC • Router • Switch • Wireless access point

Part B – T568B Pin Order

Choose the wire color for each pin of the T568B standard.  Word bank: Blue • Brown • Green • Orange • White/Blue • White/Brown • White/Green • White/Orange

Part C – Make the Cable

Number the steps from 1 to 7 for making a straight-through cable.

Part D – Safe Installation

Tick every safe practice.

Obtain permission before starting

Inspect the work area

Avoid wet locations and trip hazards

Secure ladders and wear eye protection when needed

Run data cable right beside power wiring with no separation

Leave offcuts on the floor

Part E – Troubleshoot It

A newly crimped cable fails the cable tester.

Describe two possible causes and what you would do to fix them.

Lesson 2 — Activity: Address It

Practice IPv4 settings, DHCP, verification, and troubleshooting.

How to use: click each gray field in Microsoft Word to pick an answer from the drop-down list, tick a box, or type your response.

Part A – Match the Setting

Choose the setting or term that fits each description.  Word bank: DHCP • DNS server • Default gateway • IP address • Static addressing • Subnet mask

1. Identifies a client interface on an IP network.
2. Defines which IPv4 addresses are local-network versus other-network addresses.
3. Router address used by a client to send traffic to other networks.
4. Service that resolves host/domain names to IP information.
5. Service that leases IP configuration to clients automatically.
6. Manually configured fixed-address method that must avoid conflicts and follow the approved plan.

Part B – Verify the Client

Number the steps from 1 to 6 for configuring and verifying a Windows client.

- Compare results with the authorized network plan and record relevant non-secret findings.
- Connect to the approved lab network by Ethernet or Wi-Fi.
- If authorized and Internet access is in scope, test an approved external IP and then a domain name.
- Set the adapter to DHCP or enter instructor-provided static settings.
- Run `ipconfig /all` and inspect IP, mask, gateway, and DNS.
- Run `ping 127.0.0.1`, then test the assigned address, gateway, and an authorized local host as applicable.

Part C – Fix It

Choose the best action for each symptom.  Word bank: Check DNS settings • Check cable seating, damage, NIC status, and switch/router power • Check the cable/Wi-Fi and the DHCP service on the router • Compare its settings with a working PC • Identify the conflicting device and correct the addressing plan • Replace the monitor

1. Link light is absent on a wired client.
2. The client has a 169.254.x.x address when DHCP is expected.
3. Gateway ping works, but a required domain name does not resolve.
4. A duplicate-IP warning appears.
5. One client cannot communicate while a nearby authorized client works.

Part D – Plan the Addresses

Example network: router 192.168.1.1, mask 255.255.255.0, PC 1 192.168.1.10, PC 2 192.168.1.11.

1. Write a valid static address for PC 3 that does not conflict with the example devices.

2. Write the command that shows the adapter's full settings.

3. Write the command that tests communication with the gateway.

Lesson 3 — Activity: Secure the Wi-Fi

Practice safe router access and secure wireless settings.

How to use: click each gray field in Microsoft Word to pick an answer from the drop-down list, tick a box, or type your response.

Part A – Secure or Risky?

Choose Secure or Risky for each practice.  Word bank: Risky • Secure

1. Change default router administrator credentials to approved unique credentials.
2. Use a long unique Wi-Fi passphrase separate from the router administrator password.
3. Leave an open wireless network for convenience.
4. Disable WPS or remote administration when not needed and allowed by site policy.
5. Use the obsolete WEP mode.
6. Back up the router configuration before a significant authorized change.

Part B – Match the Term

Choose the term that fits each description.  Word bank: LAN • MAC filtering • SSID • WAN • WEP • WPA2 / WPA3

1. Name used to identify a wireless network.
2. Local side/interface serving local devices.
3. Upstream side/interface toward the ISP or other network.
4. Older wireless security method that should not be selected for a new secure setup.
5. Wireless personal security standards; prefer the strongest mode supported by devices and policy.
6. Address-list feature that is not strong authentication because addresses can be spoofed.

Part C – Configure Safely

Number the steps from 1 to 6 for configuring a router.

- Test an authorized client against the approved network plan and document non-secret results.
- Obtain permission and record the current configuration without recording passwords.
- Confirm authorized WAN/LAN values and network plan with the administrator or ISP.
- Change default administrator credentials and update firmware only by approved vendor/site procedure.
- Set a non-identifying SSID and strongest supported security/passphrase under site policy.
- Keep the firewall enabled; disable unused WPS/remote administration if policy permits; save/verify the configuration.

Treat this as a plan or paper simulation. Do not reset or change a real shared router without permission.

Part D – Wireless Settings

Tick every correct practice.

Choose a clear SSID that does not reveal unnecessary personal information

Use a long, unique Wi-Fi passphrase

Disable WPS if it is not needed

Factory-reset a shared router without permission

Use the owner's full name and address as the SSID

Use an obsolete TKIP-only mode

Part E – Test the Connection

You configured the SSID Office-Lab on a router at 192.168.1.1.

List three results that show a laptop is connected correctly.

**Steps:** Complete the existing “Cable It Right” tasks and the Address It/Secure the Wi-Fi tasks. Use only instructor-approved network values. Optional cable work must use appropriate tools, safe routing, and a cable test. Client verification uses supplied sample output or an authorized lab network. Router configuration uses a lab router or paper simulation only; never write credentials in the worksheet/report and never change shared infrastructure.

**Expected output:** Completed cable tasks, six setting matches, ordered client-verification and router-configuration sequences, five troubleshooting selections, six Secure/Risky classifications, six term matches, and the existing address-planning answers and three connectivity results.
