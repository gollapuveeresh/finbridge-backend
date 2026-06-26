-- V18__normalize_investment_department.sql
UPDATE users SET department = 'investments' WHERE department = 'investment';
UPDATE dept_cases SET department = 'investments' WHERE department = 'investment';
