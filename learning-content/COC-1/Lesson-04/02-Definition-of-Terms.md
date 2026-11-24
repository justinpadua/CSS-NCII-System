# COC 1 — Lesson 4: Creation of Portable Bootable Devices

## Definition of Terms

- **ISO image:** A file representing the contents and structure of an optical disc or other installation media.
- **Bootable media:** Storage prepared with the files and boot structures needed to start a computer or installer.
- **Bootable USB:** A USB storage device prepared to start a computer using compatible firmware and boot files.
- **Partition scheme:** A format for organizing disk partitions, such as GPT or MBR.
- **GPT:** GUID Partition Table, a modern partitioning scheme commonly used for UEFI boot.
- **MBR:** Master Boot Record, a legacy partitioning scheme commonly associated with BIOS-compatible boot.
- **UEFI:** Unified Extensible Firmware Interface, firmware that initializes hardware and starts a boot process.
- **Boot menu:** A firmware menu used to select a startup device for a particular boot.
- **DiskPart:** A Windows command-line utility for managing disks, partitions, and volumes; it is not an ISO-authoring tool.
- **Oscdimg:** A Windows ADK command-line utility that creates ISO images from a folder tree; bootability requires suitable boot files and configuration.
- **Media-creation tool:** Software that obtains or writes installation media according to the operating-system publisher's instructions.
