# ARCHITECTURE

## Current Frontend
- Flask Jinja templates
- Monolithic SPA in templates/index.html
- Inline CSS and JavaScript
- ApexCharts via CDN

## Current Backend
- Flask
- SQLite3
- Single-file architecture (app.py)

## Technical Debt
- monolithic frontend
- monolithic backend
- duplicated route logic
- no reusable UI components
- incomplete blueprint migration

## Modernization Strategy

Do NOT rewrite the entire application.

Modernize incrementally:
1. extract reusable UI components
2. improve layout structure
3. modernize tables/forms/cards
4. progressively modularize frontend
5. progressively modularize routes

Preserve:
- database schema
- APIs
- business workflows
- stock calculations
- existing routes