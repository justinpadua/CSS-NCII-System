# COC 2 — Lesson 3: Network Testing and Documentation

## Activity

**Learning objective:** Sequence network checks from local to service level, select a tool for each purpose, interpret evidence cautiously, and record results.

**Materials:** Worksheet and pen; optional instructor-provided command-output cards/sample test record. A cable tester or lab client is optional and may only be used with authorization. Paper result cards are sufficient; no live network is required.

Lesson 4 — Activity: Inspect and Test

Practice the test sequence, reading results, and documentation.

How to use: click each gray field in Microsoft Word to pick an answer from the drop-down list, tick a box, or type your response.

Part A – Order the Test Sequence

Number the stages from 1 to 6 in the correct order.

- Check name resolution with `nslookup` and test an approved Internet/application service only after preceding checks and only when each is part of the requirement.
- Inspect power, cable routing/connectors, link indicators, and cable tester results where applicable.
- Test an authorized local host or required network resource.
- Check client configuration using `ipconfig /all` and test its assigned address where appropriate.
- Check local TCP/IP loopback using `ping 127.0.0.1`.
- Test the default gateway.

Part B – Match the Test

Choose the tool or command that fits each purpose.  Word bank: Cable tester • Link/activity lights • ipconfig • nslookup • ping 127.0.0.1 • ping the gateway

1. Checks a cable's wire map/continuity for common faults, not certified category performance.
2. Gives a physical-link/activity indication, but does not prove application connectivity.
3. Displays local Windows IP configuration.
4. Queries DNS name resolution.
5. Sends an ICMP echo test to the computer's own IPv4 loopback stack.
6. Tests whether the configured next-hop router responds to ICMP, if it is expected to do so.

Part C – Read the Result

Choose the best conclusion or action for each result.  Word bank: A host firewall may block ICMP; use more tests • No DHCP lease was received • Possible DNS problem • Re-terminate the cable • Secure the wireless network • The network is perfect

1. A basic tester reports an open conductor on a newly terminated patch cable.
2. A client has 169.254.x.x while it is supposed to use DHCP.
3. The client can ping the gateway, but `nslookup` for the required name fails.
4. One host does not answer ping, but its user reports the application is working.
5. A wireless access point is configured with open security in a lab where an approved secure mode is required.

Part D – Test Record

Tick every item that belongs in the test record.

Device names

IP addresses

Cable labels

Configuration changes

Unresolved issues

Pass/fail and corrective action

A learner's favorite website

Passwords written in plain text

Part E – Write the Report Entry

Test: ping the gateway. Result: Request timed out.

Write two further checks and one corrective action, then say what you would record.

**Steps:** Use sample result cards or approved lab values. For each conclusion, state what the result does and does not prove. Complete the test record with the test, expected result, actual result, corrective action, and retest. Redact sensitive values from screenshots and do not record passwords.

**Expected output:** Ordered six-stage sequence, six test-purpose matches, five result interpretations, completed test-record checklist, and a report entry for the ping-timeout scenario.
