# COC 2 — Lesson 3: Network Testing and Documentation

## Video Lessons

1. **[PING Command — Troubleshooting Networks — PowerCert Animated Videos](https://www.youtube.com/watch?v=IIicPE38O-s)**  
   Explains how ping tests reachability and how it can assist with network troubleshooting. A successful ping confirms an ICMP reply, not internet speed or that every application is working.

2. **[IPCONFIG Explained — Flush DNS Cache — PowerCert Animated Videos](https://www.youtube.com/watch?v=ZKhorleA5aA)**  
   Demonstrates how to view IP address, subnet mask, default gateway, and DNS-related information using Windows ipconfig commands.

3. **[Network Troubleshooting using PING, TRACERT, IPCONFIG, NSLOOKUP COMMANDS](https://www.youtube.com/watch?v=kTkF3hp9hSA)**  
   Demonstrates several Windows command-line tools, including ping, tracert, ipconfig, and nslookup. The channel has a very small audience; use it as a supplementary demonstration and verify commands in the instructor's lab environment.

4. **[CSS NC II COC 2 — Set Up Computer Network](https://www.youtube.com/watch?v=fcFmsmXaoW4)**  
   Relevant testing section begins around 33:06 and demonstrates checking IP configuration and pinging devices on the local network.

### Testing reminders
- Combine command-line tests with physical inspection of cables, link lights, ports, and network-device power.
- A failed ping can result from a disconnected network, wrong addressing, routing problems, a firewall, or ICMP filtering. Do not conclude that a device is faulty from one ping result alone.
- `ipconfig` displays local Windows IP configuration. `nslookup` tests DNS name resolution. Record the exact command and output relevant to the fault; redact private or sensitive information before sharing reports.
