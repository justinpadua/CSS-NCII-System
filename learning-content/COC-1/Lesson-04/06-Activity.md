# COC 1 — Lesson 4: Creation of Portable Bootable Devices

## Activity

LESSON 4 – ACTIVITY: BOOT IT UP

**Learning objective:** Sequence safe bootable-media preparation and distinguish disk-management commands from ISO creation.

**Materials:** Worksheet and pen. Optional instructor demonstration on an authorized lab computer using an approved disposable USB with no needed data. Paper simulation is the default; do not run DiskPart or erase a device without direct authorization.

Practice creating and testing bootable media.

How to use: click each gray field in Microsoft Word to pick an answer from the drop-down list, tick a box, or type your response.

Part A – Order the Rufus Procedure

Number the steps from 1 to 7 in a safe order.

- Click START only after checking the selected drive and settings; acknowledge the erase warning only after confirming the USB is the intended disposable device.
- Obtain a valid operating-system ISO from its official source.
- Wait for Rufus to report completion, safely eject the USB, and test it from the target computer's boot menu when authorized.
- Choose the partition scheme appropriate to the target's firmware and OS instructions.
- Insert the USB after backing up its contents.
- Open Rufus and select the correct USB device.
- Select the ISO image in Rufus and review the options.

Part B – Match the Command

Choose the command or tool for each purpose.  Word bank: assign • diskpart • list disk • list volume • oscdimg • select disk

1. Starts the Windows disk-management command environment.
2. Displays connected disks so their numbers and capacities can be checked.
3. Chooses a disk for subsequent commands; selecting the wrong disk is dangerous.
4. Displays volumes and their drive letters or labels.
5. Assigns a drive letter to a selected volume.
6. A Windows ADK utility that can build an ISO from a prepared folder tree; bootability still depends on correct boot files and options.

Part C – True or False

Choose True or False for each statement.

1. An ISO file is already a bootable USB as soon as it is downloaded.
2. Writing installation media can erase the selected USB drive.
3. A generic data ISO made from a folder is necessarily bootable.
4. DiskPart manages disks, partitions, and volumes; it is not an ISO-authoring tool.
5. Before a disk command or media write, verify the target device and authorization.

Correct each false statement in one sentence.

Part D – Customer Checklist

Before making a bootable USB for a customer:

Write three things you must check or tell the customer first.

**Steps:** Complete the worksheet and customer checklist. If demonstrating, use only an approved disposable USB, verify the target before starting, and do not execute destructive DiskPart commands. Safely eject only after Rufus reports completion. Paper simulation is complete without hardware.

**Expected output:** Safe seven-step Rufus sequence, six command/tool matches, five True/False responses with corrections to false statements, and three customer checks or disclosures.
