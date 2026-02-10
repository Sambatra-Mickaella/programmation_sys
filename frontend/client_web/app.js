// Simule la navigation et les actions côté front
function goToHome() {
  window.location.href = 'home.html';
}

function goToLogin() {
  window.location.href = 'login.html';
}

function handleLogin(e) {
  e.preventDefault();
  // Simule un login réussi
  goToHome();
}

function handleLogout() {
  goToLogin();
}

function handleDownload(filename) {
  alert('Téléchargement de ' + filename);
}

function handleDelete(filename) {
  if (confirm('Supprimer ' + filename + ' ?')) {
    alert('Fichier supprimé (simulation)');
    // Ici, on pourrait retirer la ligne du tableau côté JS
  }
}

// Pour la page d'accueil, simule une liste de fichiers
function renderFiles() {
  const files = ['monfichier.txt', 'metyfona.txt'];
  const tbody = document.getElementById('files-tbody');
  if (!tbody) return;
  tbody.innerHTML = '';
  files.forEach(f => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="py-2 px-4 border-b">${f}</td>
      <td class="py-2 px-4 border-b space-x-2">
        <button onclick="handleDownload('${f}')" class="bg-green-500 text-white px-3 py-1 rounded hover:bg-green-600">Télécharger</button>
        <button onclick="handleDelete('${f}')" class="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600">Supprimer</button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}
