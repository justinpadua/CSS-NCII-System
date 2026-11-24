# AI Agent Instructions

## 1. Main Reference

- Always read SYSTEM_DESIGN.md before implementing features.
- Treat SYSTEM_DESIGN.md as the main source of truth.
- Do not invent or change system requirements.
- Ask for clarification when requirements are incomplete.

## 2. Technology Stack

- Frontend: React.js with Vite
- Backend: Java + Spring Boot REST API
- Database: MySQL
- Styling: Tailwind CSS
- Authentication: Spring Security

Do not replace these technologies without approval.

## 3. Development Rules

- Build the system one feature at a time.
- Plan before implementing.
- Do not implement multiple unrelated features in one task.
- Reuse existing components and code.
- Keep the code readable and beginner-friendly.
- Do not unnecessarily modify working features.

## 4. Testing Rules

- Test every implemented feature.
- Fix errors before proceeding.
- Never claim a feature is complete without testing.
- Follow the testing requirements in SYSTEM_DESIGN.md.

## 5. Database and Security

- The Spring Boot backend must enforce important business rules.
- Never trust frontend validation alone.
- Protect student and admin data.
- Never expose correct answers before they are allowed.

## 6. Communication

- Explain the implementation plan before coding.
- Explain files created and modified.
- Explain important code changes.
- Ask for approval before major architectural changes.
- Stop after completing the requested task.

## 7. Documentation

- Maintain PROGRESS.md.
- Update documentation when features change.
- Never mark untested features as complete.
