try{var m=localStorage.getItem("tg_theme")||"light";document.documentElement.dataset.theme=m==="auto"?(matchMedia("(prefers-color-scheme:dark)").matches?"dark":"light"):m}catch(e){}
