TASK GULLAK PUBLIC WEBSITE — INSTALLATION NOTES
================================================

This package is a static HTML/CSS/JavaScript public website.

FILES
- index.html             Public homepage
- how-it-works.html      How it works
- features.html          Features
- faq.html               FAQ
- support.html           Support form (opens email client)
- about.html             About
- privacy.html           Privacy policy starter template
- terms.html             Terms starter template
- 404.html               Custom not-found page
- assets/style.css       Shared responsive styling and animations
- assets/app.js          Navigation, reveal animations, FAQ and support form

IMPORTANT: PRESERVING YOUR EXISTING ADMIN PANEL
------------------------------------------------
I have not received your existing website source code, so this package cannot safely merge or test your current admin implementation.

Before deploying:
1. Back up your current website and hosting configuration.
2. Identify exactly where the current admin panel files and routing live.
3. Keep the existing admin implementation and its authentication unchanged.
4. Put the admin panel under /admin (or preserve its current working admin route).
5. Deploy these public files as the root/public site, but do NOT overwrite or delete the existing admin files.
6. Configure the host so /admin and its assets/API routes continue to resolve to the existing admin application.
7. Configure unknown public paths to serve /404.html with an HTTP 404 status where your host supports it.

If the current admin panel is served from the root index.html, replacing that file without changing routing WILL affect the admin panel. Do not deploy until the routes are separated.

HOSTING NOTES
-------------
This is plain static HTML. Links use root-relative paths (for example /faq.html), so deploy the package at the domain root or adjust paths if hosted in a subfolder.

For Apache, an optional .htaccess can be used to serve 404.html for missing paths. For Netlify, add a _redirects file. For Firebase Hosting, configure rewrites/not-found handling in firebase.json. Do not copy a hosting config blindly; merge with your existing config to preserve /admin and API rewrites.

CUSTOMISE BEFORE PUBLISHING
---------------------------
1. Search for APP_DOWNLOAD_URL_HERE and set your real app download / Play Store URL.
2. Search for SUPPORT_EMAIL_HERE and replace it with your verified support email.
3. In assets/app.js, replace SUPPORT_EMAIL_HERE in the mailto handler.
4. Review privacy.html and terms.html carefully. They are starter templates, not legal advice.
5. Check the referral amounts and commission in the app before publishing; update the copy if programme rules change.
6. Replace the sample wallet figures in the phone mockup with non-personal illustrative values or remove them.

The “Get the App” button currently has a placeholder link. Add data-app-link handling or replace href="#" in index.html with your official URL.
