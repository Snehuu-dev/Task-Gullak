const navToggle=document.querySelector('.menu-btn');
const navLinks=document.querySelector('.navlinks');
if(navToggle&&navLinks){navToggle.addEventListener('click',()=>navLinks.classList.toggle('show'))}
const observer=new IntersectionObserver(entries=>{entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('visible');observer.unobserve(entry.target)}})},{threshold:.12});
document.querySelectorAll('.reveal').forEach(el=>observer.observe(el));
document.querySelectorAll('.faq-q').forEach(btn=>btn.addEventListener('click',()=>{const item=btn.closest('.faq-item');item.classList.toggle('open');btn.querySelector('span:last-child').textContent=item.classList.contains('open')?'−':'+'}));
const year=document.querySelectorAll('[data-year]');year.forEach(el=>el.textContent=new Date().getFullYear());

// Configure your official app download URL here before publishing.
const APP_DOWNLOAD_URL='APP_DOWNLOAD_URL_HERE';
document.querySelectorAll('[data-app-link]').forEach(link=>{link.href=APP_DOWNLOAD_URL==='APP_DOWNLOAD_URL_HERE'?'#':APP_DOWNLOAD_URL; if(APP_DOWNLOAD_URL==='APP_DOWNLOAD_URL_HERE') link.addEventListener('click',e=>{e.preventDefault();alert('Add your official app download URL in assets/app.js first.');});});
const form=document.querySelector('#support-form');
if(form){form.addEventListener('submit',e=>{e.preventDefault();const data=new FormData(form);const subject=encodeURIComponent('Task Gullak support: '+data.get('subject'));const body=encodeURIComponent('Name: '+data.get('name')+'\\nEmail: '+data.get('email')+'\\n\\n'+data.get('message'));window.location.href='mailto:SUPPORT_EMAIL_HERE?subject='+subject+'&body='+body;});}
