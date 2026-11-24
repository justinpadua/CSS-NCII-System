# COC 1 — Lesson 5: Installation of System and Application Software

## Definition of Terms

LESSON 5 – DEFINITION OF TERMS

Study these key terms before starting the lesson.

BIOS – Basic Input/Output System; firmware that initializes hardware during startup.

UEFI – Unified Extensible Firmware Interface; the modern firmware that initializes hardware and starts the operating system.

Boot Order – The sequence of devices the computer tries to start from.

Secure Boot – A UEFI feature that helps ensure only trusted software loads at startup.

Operating System Installation – The process of placing an operating system on a drive so the computer can run.

Clean Installation – Installing an operating system on a freshly prepared drive instead of upgrading.

Installation Media – A USB drive or disc containing the files needed to install an operating system.

Partition – A section of a disk that is treated as a separate storage area.

Volume – A formatted storage area that can be assigned a drive letter.

GPT / MBR – Partition styles; GPT is commonly used with UEFI and MBR with legacy BIOS.

EFI System Partition – A small partition that holds boot files on UEFI systems.

Recovery Partition – A partition that holds tools to repair or restore the system.

Disk Management – A Windows tool for viewing and managing disks, volumes, and partitions.

Format – Preparing a volume with a file system so it can store files.

Device Driver – Software that allows the operating system to communicate with a hardware device.

Device Manager – A Windows tool that lists hardware and shows unresolved device problems.

System Software – Software such as the operating system and drivers that runs the computer.

Application Software – Programs installed after the operating system, such as an office suite or browser.

Backup – A copy of important files stored safely before changes are made.

Authorization – Permission from the owner before installing, reinstalling, or erasing data on a computer.

5.1 Apply BIOS/UEFI Configuration, Disk Management, and Driver Installation to Set Up a Fully Functional System

Figure 5.1. Example of a BIOS/UEFI setup screen and a motherboard used during system configuration.

BIOS (Basic Input/Output System) and UEFI (Unified Extensible Firmware Interface) are firmware interfaces that initialize hardware and help the computer locate a bootable operating system. UEFI is the modern firmware environment used by most current computers.

A. Common BIOS/UEFI Settings

• System date and time – keeps the firmware clock accurate.

• Boot priority – selects the order in which the computer checks devices for a bootable operating system.

• UEFI/Legacy or CSM mode – controls the firmware boot compatibility mode. Use the mode supported by the operating system and installation media.

• Secure Boot – helps prevent unauthorized boot software from loading. Change it only when required and authorized.

• Storage configuration – detects SATA or NVMe drives and may include controller settings. Avoid changing controller mode without understanding the effect on the installed OS.

• Hardware monitoring – displays information such as CPU temperature, fan speed, and system voltage.

B. Safe BIOS/UEFI Configuration Procedure

Read the motherboard or computer manufacturer's manual to identify the correct setup key (often Delete, F2, Esc, or a function key).

Restart the computer and press the setup key during startup.

Check that the processor, memory, and storage devices are detected.

Set the date and time if needed.

Review boot priority and select the correct UEFI USB installer when installing an OS.

Keep security and storage settings at recommended defaults unless a documented requirement calls for a change.

Save changes and exit. If the system behaves unexpectedly, return to setup and restore the previous settings or manufacturer's defaults.

C. Disk Management and Partitioning

A disk is a physical storage device such as an HDD or SSD. A partition is a logical section of a disk. A volume is a formatted storage area that the operating system can use. Disk Management in Windows can initialize disks, create and format volumes, assign drive letters, and view partition status.

• GPT (GUID Partition Table) – commonly used with UEFI systems; supports large disks and many partitions.

• MBR (Master Boot Record) – older partition style commonly associated with legacy BIOS booting.

• Formatting prepares a partition with a file system, such as NTFS. Formatting or deleting a partition can erase its contents.

D. Driver Installation

A device driver is software that allows the operating system to communicate with a hardware device. Examples include chipset, graphics, audio, LAN/Wi-Fi, Bluetooth, printer, and storage drivers.

Identify the exact computer model or motherboard model and the operating system version.

Visit the manufacturer's official support website.

Download the correct driver package for the device and operating system.

Install the driver following the manufacturer's instructions. Restart if requested.

Open Device Manager and check that the device is detected and has no relevant warning symbol.

Test the device—for example, test sound output, network connectivity, display resolution, or printer operation.

E. Practical Example

Scenario: A newly assembled desktop has a blank SSD and a Windows installation USB. The technician checks UEFI and confirms that the SSD is detected, selects the USB installer from the boot menu, installs Windows to the intended SSD, then installs chipset, network, audio, and graphics drivers. The technician opens Device Manager and tests internet access, sound, and display output.

5.2 Configure BIOS/UEFI Settings, Install an Operating System, Manage Disk Partitions, and Install All Necessary System and Device Drivers

Figure 5.2. Example of an operating-system installation screen beside a disk partition management view.

An operating system (OS) manages hardware resources, files, memory, processes, and applications. Examples include Microsoft Windows and Linux distributions. A clean installation places a fresh copy of the OS on a selected drive and may erase existing files.

A. Requirements Before Installation

• A compatible computer that meets the operating system's requirements.

• A genuine bootable USB installer or other authorized installation media.

• A valid license or activation entitlement where required.

• A backup of important files and a record of the existing disk layout.

• Internet access or downloaded manufacturer drivers if network drivers are not included in the installer.

B. Operating System Installation Procedure

Back up the user's files and confirm permission to install or erase the operating system.

Check hardware compatibility and confirm the correct target drive.

Create installation media using the operating system vendor's official tool or instructions.

Connect the USB installer and open the computer's boot menu or UEFI setup.

Choose the USB device in the appropriate UEFI boot mode.

Follow the installer prompts to select language, keyboard, edition, and installation type.

At the disk selection screen, identify the correct target drive by its capacity and model. Do not delete partitions on other drives.

Create or select the required partitions according to the installation method and OS guidance. For a UEFI Windows installation, GPT is normally used.

Continue installation and allow the computer to restart. Remove the USB or restore the internal drive as the first boot device when appropriate.

Complete the initial setup, create or sign in to an authorized user account, and connect to the network.

C. Partition Management After Installation

• Open Disk Management to view the disk, EFI/system partitions, recovery partitions, and data volumes.

• Assign drive letters to suitable data volumes when needed.

• Create a new volume only from unallocated space and only after confirming the correct disk.

• Do not delete EFI, recovery, or system partitions unless following a verified recovery or deployment procedure.

D. System and Application Software

After the OS is installed, apply operating-system updates and install required drivers. Then install authorized application software such as an office suite, web browser, PDF reader, media player, or security software. Use official publishers' websites or trusted organizational deployment sources.

E. Practical Example and Verification Checklist

A computer is prepared for a student. Windows is installed on the intended SSD, the correct network and graphics drivers are installed, updates are applied, and an office application and browser are installed. The technician verifies that the computer can restart, connect to the internet, play audio, display the correct resolution, recognize storage, and open the required applications.

• Operating system starts normally.

• Correct disk capacity and partitions are visible.

• Device Manager shows no unresolved device problems.

• Network, audio, display, keyboard, mouse, and USB ports work.

• Required applications open and are updated.

• Installation details and any remaining issues are documented.
