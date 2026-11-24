# COC 1 — Lesson 4: Creation of Portable Bootable Devices

## Definition of Terms

LESSON 4 – DEFINITION OF TERMS

Study these key terms before starting the lesson.

ISO Image – A file containing the data and structure of a disc or installation media.

Bootable Device – A storage device that contains the files and boot structures required to start a computer.

Bootable USB Drive – A USB flash drive prepared to start a computer and load an installer or recovery tool.

Recovery Drive – Bootable media used to repair or restore a computer system.

Linux Live USB – A bootable USB drive that runs Linux without installing it on the computer.

Rufus – An application used to create bootable USB installation media from ISO images.

Third-Party Application – Software made by a company other than the operating system's maker, such as ImgBurn, PowerISO, or AnyBurn.

Partition Scheme – The method used to organize partitions on a disk, such as GPT or MBR.

GPT (GUID Partition Table) – A partition scheme used by most modern UEFI systems.

MBR (Master Boot Record) – A partition scheme used by compatible legacy BIOS systems.

UEFI – The modern firmware interface used to initialize hardware and boot an operating system.

Legacy BIOS – The traditional firmware that initializes hardware during startup.

Boot Menu – A startup menu used to choose the device from which to boot.

Command Prompt – A text-based Windows interface where commands are typed.

diskpart – A command utility used to manage disks, volumes, and partitions.

oscdimg – A command-line utility from the Windows ADK used to create ISO images.

UDF (Universal Disk Format) – A file system commonly used for optical disc images.

Mounting – Making an image file accessible as if it were a disc.

4.1 How to Create a Functional ISO Image and Bootable Device

A. What is an ISO Image?

An ISO image is a file containing the data and structure of a disc or installation media.

Uses:

Operating system installation.

Software distribution.

System recovery.

Backup of installation media.

Creating bootable USB drives.

B. What is a Bootable Device?

A bootable device is a storage device that contains the files and boot structures required to start a computer.
