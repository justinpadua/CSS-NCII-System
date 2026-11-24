COC 1 — LESSON 6: Discussion
6.1 Discuss How to Apply Stress Testing Techniques to Evaluate System Performance and Stability
Stress testing is a method of applying a demanding workload to one or more computer components to evaluate stability, cooling, and performance. It can help reveal overheating, unstable memory, graphics errors, power problems, and system crashes. A stress test does not guarantee that a computer will never fail; it provides evidence about the system under the tested conditions.
A. Types of Stress Tests
• CPU test – places a heavy computational workload on the processor. Observe temperature, clock speed, utilization, and errors.
• GPU test – renders demanding graphics to check graphics stability, cooling, and artifacts.
• Memory test – checks RAM for errors that may cause crashes or corrupted data.
• Storage test – checks drive health, read/write behavior, and possible errors. Back up important data before any test that writes to a drive.
• Power or combined system test – loads multiple components at once and can reveal power delivery or cooling weaknesses.
B. Common Testing and Monitoring Software
• OCCT – offers CPU, GPU, memory, and power-related stability tests.
• HWiNFO – displays hardware sensors such as temperatures, fan speeds, clock speeds, and power readings.
• MemTest86 – a bootable memory diagnostic tool used to check RAM for errors.
• Manufacturer utilities – some CPU, GPU, and storage manufacturers provide their own diagnostics.
C. Preparing for a Stress Test
Confirm the computer is correctly assembled and the cooling system is working.
Check that fans spin, vents are clear, and the computer is on a stable power source.
Record the CPU, GPU, RAM, storage, operating system, and driver versions.
Close unnecessary programs and save work.
Open a monitoring utility and note idle temperatures and other baseline readings.
Choose a test that matches the component being evaluated and use a conservative duration suitable for the task.
Explain the test to the owner or supervisor and obtain permission where required.
D. Interpreting Test Results
• Pass/no reported errors – the test completed for the selected duration without reported errors or instability. This does not prove the system is fault-free.
• Overheating – temperatures approach or exceed the manufacturer's limits, or the system throttles or shuts down. Check cooler mounting, thermal interface material, airflow, and fan operation.
• Calculation or memory errors – may indicate unstable settings, faulty RAM, CPU instability, or other hardware problems. Retest methodically after checking settings.
• Artifacts or GPU errors – may indicate GPU instability, driver problems, overheating, or power issues.
• Unexpected shutdown or restart – may be related to temperature, power delivery, hardware, or software. Record the exact conditions and investigate before repeating.
E. Practical Example
A technician receives a desktop that restarts during gaming. The technician records its specifications, checks the fans and airflow, monitors idle temperatures, and runs a controlled GPU test while observing temperature and system behavior. If the system restarts, the technician stops the test, records the time and readings, checks power and cooling, and performs further diagnosis rather than repeatedly stressing the system.
6.2 Perform a Stress or Burning Test on a Computer System, Record the Results, and Analyze Any Issues Observed During Testing
A burn-in test is a period of sustained operation under a selected workload. It is used to check whether a computer remains stable over time. A technician should choose an appropriate test duration, monitor the system continuously, and document the results.
A. Equipment and Software
• Computer under test with working CPU cooler, GPU cooling, case fans, and power supply.
• Monitor, keyboard, and mouse.
• OCCT or another reputable component-specific test utility.
• HWiNFO or a similar sensor-monitoring utility.
• A test record sheet or spreadsheet.
B. Step-by-Step Stress/Burn-In Test Procedure
Obtain permission and confirm that important work is saved.
Record the computer model and specifications, operating system, driver versions, and date.
Inspect the case, fans, cooler, cables, and vents. Do not open the power supply unit.
Start the computer and allow it to reach a normal idle state.
Open the monitoring software and record baseline CPU/GPU temperatures, fan speeds, and utilization.
Select the appropriate test (CPU, GPU, memory, or combined) and set a conservative test duration based on the task and hardware guidance.
Start the test and continuously observe temperatures, utilization, fan behavior, error messages, and system stability.
Stop the test immediately if unsafe temperatures, errors, unusual noise or smell, artifacts, or instability occurs.
At the end of the test, stop the workload normally and save the software report or screenshots.
Compare the results with manufacturer guidance and the baseline readings. Identify possible causes of any issue.
Write a conclusion and recommendations, including whether the system passed the selected test conditions or needs further diagnosis.
C. Test Record Template

| Component/test | Tool and version | Settings and duration | Baseline and peak readings | Errors or behavior | Result and follow-up |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |

Enter actual observations from the supervised test. Do not copy example results or use a universal temperature limit. Compare readings with the hardware manufacturer's guidance and the test tool's instructions.

D. Interpreting Observed Issues
• If the CPU temperature rises rapidly and the computer throttles, inspect the cooler installation, fan operation, airflow, and thermal interface material.
• If memory errors appear, record the test and settings, then check RAM seating, supported memory settings, and individual modules using an appropriate diagnostic procedure.
• If the GPU displays artifacts, check GPU temperature, driver version, power connectors, and stability at default settings.
• If the system unexpectedly shuts down, stop testing and investigate cooling, power supply, cables, and hardware condition.
E. Documentation Requirements
• Computer identification and hardware specifications.
• Test software and version, selected test type, settings, and duration.
• Start and end time, idle and peak temperatures, and relevant sensor readings.
• Errors, crashes, artifacts, throttling, unusual noises, or other observations.
• Final result, interpretation, corrective action, and technician's name/signature.


