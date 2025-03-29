document.addEventListener('DOMContentLoaded', function() {
    // Sidebar toggle functionality
    document.getElementById('sidebarCollapse').addEventListener('click', function() {
        document.getElementById('sidebar').classList.toggle('active');
        document.getElementById('content').classList.toggle('active');
    });
    
    // Submenu toggle functionality
    const dropdowns = document.querySelectorAll('.dropdown-toggle');
    dropdowns.forEach(dropdown => {
        dropdown.addEventListener('click', function(event) {
            const submenuId = this.getAttribute('href');
            if (submenuId && submenuId.startsWith('#')) {
                event.preventDefault();
                const submenu = document.querySelector(submenuId);
                if (submenu) {
                    submenu.classList.toggle('show');
                }
            }
        });
    });
    
    // Add active class to current menu item based on URL
    const currentLocation = window.location.pathname;
    const menuItems = document.querySelectorAll('.sidebar ul li a');
    menuItems.forEach(item => {
        if (item.getAttribute('href') === currentLocation) {
            item.parentElement.classList.add('active');
            
            // If it's in a submenu, expand the parent menu
            const submenu = item.closest('.collapse');
            if (submenu) {
                submenu.classList.add('show');
                const parentToggle = document.querySelector(`[href="#${submenu.id}"]`);
                if (parentToggle) {
                    parentToggle.setAttribute('aria-expanded', 'true');
                    parentToggle.parentElement.classList.add('active');
                }
            }
        }
    });
});
