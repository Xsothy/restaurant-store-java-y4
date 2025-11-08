# Frontend Setup - Tailwind CSS

This project uses Tailwind CSS for styling with a pre-build approach instead of using the CDN.

## Structure

```
/
├── src/main/
│   ├── tailwind/
│   │   └── input.css          # Tailwind input file with directives
│   ├── resources/
│   │   ├── static/css/
│   │   │   └── tailwind.css   # Generated CSS (committed to repo)
│   │   └── templates/
│   │       ├── fragments/
│   │       │   └── layout.html # Reusable layout fragments
│   │       ├── layout/
│   │       │   └── base.html  # Base layout template (optional)
│   │       └── *.html         # Page templates
├── package.json               # NPM dependencies
├── tailwind.config.js         # Tailwind configuration
└── pom.xml                    # Maven build with frontend-maven-plugin
```

## Layout System

The project uses Thymeleaf fragments for maintainable layouts:

### Available Fragments

1. **head(title)** - HTML head with CSS link
2. **header** - Full header with customer info and logout
3. **header-simple** - Simple header with back to menu button
4. **footer** - Footer with copyright
5. **api-config** - API endpoint configuration script
6. **auth-utils** - Authentication utility functions

### Usage Example

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <th:block th:replace="~{fragments/layout :: head('Page Title')}"></th:block>
</head>
<body class="bg-white min-h-screen">
    <th:block th:replace="~{fragments/layout :: header}"></th:block>
    
    <main>
        <!-- Your content here -->
    </main>
    
    <th:block th:replace="~{fragments/layout :: footer}"></th:block>
    <th:block th:replace="~{fragments/layout :: api-config}"></th:block>
    <th:block th:replace="~{fragments/layout :: auth-utils}"></th:block>
</body>
</html>
```

## Building CSS

### During Maven Build
The CSS is automatically built during Maven's build lifecycle via the `frontend-maven-plugin`:

```bash
mvn clean package
```

This will:
1. Install Node.js and npm locally (in the project)
2. Run `npm install` to install dependencies
3. Run `npm run build` to generate Tailwind CSS

### Manual Build
For development, you can build manually:

```bash
npm install
npm run build
```

### Watch Mode (Development)
For live CSS rebuilding during development:

```bash
npx tailwindcss -c tailwind.config.js -i ./src/main/tailwind/input.css -o ./src/main/resources/static/css/tailwind.css --watch
```

## Tailwind Configuration

The `tailwind.config.js` scans all HTML templates for class names:

```javascript
content: [
  './src/main/resources/templates/**/*.html',
  './src/main/resources/static/js/**/*.js',
]
```

Only classes used in these files will be included in the final CSS, keeping the bundle size small.

## Why Pre-build Instead of CDN?

1. **Performance** - No external dependency, faster page loads
2. **Production Ready** - Minified and optimized CSS
3. **Offline Development** - No internet required
4. **Bundle Size** - Only includes used classes (tree-shaking)
5. **Consistent Builds** - Locked versions, reproducible builds
