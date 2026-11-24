# COC 2 — Lesson 1: Network Concepts and Planning

## Discussion

### 1.1 Understand the requirement before installation

A network lets computers and other devices exchange data or share services. Before installation, establish the purpose, number and location of devices, required wired and wireless coverage, available uplink, security requirements, and cable routes. Record assumptions and obtain approval for the proposed design.

A **PAN** covers nearby personal devices; a **LAN** serves a limited site such as a room or building; a **MAN** spans a metropolitan area; and a **WAN** connects networks across larger distances. These terms describe scope, and real network boundaries depend on the organization and service design. A **star topology** connects endpoints to a central switch or access point; this is common in small LANs and makes a single link easier to isolate. Other topologies have different link and failure characteristics. A topology plan should show devices, links, labels, and network boundaries.

### 1.2 Addressing and network architecture

An IP address identifies an interface on an IP network. IPv4 uses 32-bit addresses commonly written as four decimal numbers separated by dots, for example `192.168.10.20`. IPv6 uses 128-bit addresses written in hexadecimal groups separated by colons, for example `2001:db8::20`; `2001:db8::/32` is reserved for documentation examples. These examples are not instructions for assigning addresses. Use the approved addressing plan. Subnet prefixes or masks identify which addresses belong to a network; gateways and routes enable communication beyond the local subnet.

In a **peer-to-peer** model, endpoints can share resources directly without requiring a dedicated server for every service. In a **client-server** model, client devices request services from server systems, which can centralize resources and administration. Select the architecture based on actual service, security, scale, and support requirements; neither label alone guarantees security.

### 1.3 Identify the network equipment

- **Modem or ONT:** terminates the service provider's connection. Some units combine several network functions.
- **Router:** forwards traffic between IP networks and commonly serves as the LAN gateway. Consumer routers may also provide DHCP, firewall, and Wi-Fi functions.
- **Switch:** connects wired devices within a LAN. It does not normally replace a router when traffic must reach another IP network.
- **Wireless access point (AP):** provides Wi-Fi access to a wired LAN. An AP may be built into a router.
- **Network interface card (NIC):** the wired or wireless network interface in a computer or other device.
- **Patch panel and keystone jack:** provide organized, fixed termination points for structured cabling.

### 1.4 Plan cabling and materials

List the endpoints and estimate cable routes and lengths before cutting cable. Select cable, connectors, jacks, and tools that are compatible with the installation plan. Cat5e and Cat6 are common twisted-pair Ethernet cable categories; the required category and supported data rate depend on the complete channel, installed components, distance, and equipment. Do not promise a speed based on the cable label alone.

A typical toolkit may include a cable cutter/stripper, crimping tool, punch-down tool for jacks, and cable tester. Keep cable runs protected, labeled, and away from avoidable crushing, sharp bends, moisture, and trip hazards. Follow site rules for separation from electrical wiring; ask a qualified person when the route is unclear. Use ladders and eye protection as required and handle tools safely.

### 1.5 Terminate and test a straight-through cable

T568A and T568B define conductor pinouts. For a common straight-through patch cable, use the same standard at both ends and follow the site standard. Traditional crossover cabling uses different standards at the ends, although many modern devices support Auto-MDIX.

For T568B, the conductor order at pins 1–8 is white/orange, orange, white/green, blue, white/blue, green, white/brown, brown.

1. Measure and cut the cable to the planned length.
2. Strip only enough jacket to arrange conductors without damaging their insulation; preserve the pair twists as close to the termination as the connector permits.
3. Arrange all eight conductors in the chosen standard's order and trim them evenly.
4. Insert the conductors fully and confirm the jacket is captured by the plug's strain-relief area.
5. Crimp with the compatible tool, then terminate the other end using the same standard for a straight-through cable.
6. Test the cable and inspect the wire map for opens, shorts, or crossed conductors.
7. Label the cable and route it safely.

A basic continuity tester can identify many wiring faults, but it does not certify the channel's category performance or data rate. Record the test result and replace or reterminate a failed cable before using it in the installation.


