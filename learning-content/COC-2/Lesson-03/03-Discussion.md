# COC 2 — Lesson 3: Network Testing and Documentation

## Discussion

This Discussion follows the source lesson on inspecting and testing a configured network. Test only networks and devices you are authorized to inspect. A single successful test does not prove that every service works, and a failed ping may mean ICMP is filtered rather than that the whole network is unavailable.

### 3.1 Inspect the physical installation

1. Confirm that switches, routers, access points, and computers are powered, ventilated, labeled, and securely placed.
2. Inspect cable routes, connectors, strain relief, raceways, and trip hazards. Check that cables are not sharply bent, crushed, or exposed to avoidable damage.
3. Check link and activity indicators on network interface cards (NICs), switches, and routers. A light can show a physical link or activity, but it does not prove that IP settings or applications work.
4. Check wireless coverage only in required work areas. Do not record Wi-Fi passwords in a report.

### 3.2 Verify client configuration

On representative client computers, inspect the IP address, subnet mask, default gateway, and DNS server. On Windows, `ipconfig /all` displays adapter configuration. Compare the values with the approved network plan. A `169.254.x.x` IPv4 address is link-local; it can indicate that a DHCP lease was not obtained, so check the physical link, DHCP service, and configuration before concluding why it occurred.

### 3.3 Test in a useful order

Use a sequence that narrows the fault from local components toward the required service. The precise sequence can vary with the network and its requirements.

1. Inspect power, cabling, link indicators, and cable tester results where applicable.
2. Run `ping 127.0.0.1` to check the local TCP/IP loopback stack.
3. Check adapter configuration with `ipconfig /all`; test the computer's own assigned address if appropriate.
4. Test the default gateway with `ping <gateway-address>` if the gateway is expected to answer ICMP.
5. Test another authorized local host or required network resource.
6. Use `nslookup <name>` to check DNS resolution. Test internet or application access only when it is part of the approved requirement.

A ping reply confirms that an ICMP echo reply was received from that address at that time. It does not measure internet speed or guarantee that an application works. A timeout may be caused by a firewall or ICMP filtering. Combine tests and interpret each result in context.

### 3.4 Locate common faults

- **No link light:** check power, cable, connector termination, NIC, and switch port.
- **Link is present but no expected DHCP address:** check VLAN or port configuration where applicable, DHCP availability, and whether the client is set to obtain an address automatically.
- **Gateway responds but a name does not resolve:** inspect DNS server settings and `nslookup` results.
- **Name resolves but the required application fails:** check the service, application configuration, firewall policy, and exact requirement. Do not assume DNS is the only possible cause.
- **A single ping fails:** check whether the target is configured to answer ICMP and use another appropriate test before declaring an outage.

### 3.5 Document findings and retest

Record the date and location, device names or approved identifiers, relevant IP settings, cable labels, test commands or tools, expected and actual results, symptoms, corrective action, and retest results. Record unresolved issues and who should follow up. Do not include passwords, secret keys, or unnecessary personal information. After an authorized correction, repeat the relevant checks and record the result.

### Example

A workstation has a link light and receives an address in the planned subnet. It can ping the gateway, but `nslookup` cannot resolve the required internal server name. The technician records the command and result, checks the configured DNS server against the network plan, and asks the network administrator to confirm the DNS record or service. The technician retests name resolution after the approved correction and documents both results.

