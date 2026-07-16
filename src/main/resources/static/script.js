// =========================================================================
// 1. MAPEAMENTO DE ELEMENTOS (Ponte entre HTML e JavaScript)
// =========================================================================
const videoWebcam = document.getElementById('webcam');
const canvasFoto = document.getElementById('foto-capturada');
const previewImagem = document.getElementById('preview-imagem');

const btnIniciarCam = document.getElementById('btn-iniciar-cam');
const btnTirarFoto = document.getElementById('btn-tirar-foto');
const btnValidar = document.getElementById('btn-validar');
const inputArquivo = document.getElementById('input-arquivo');

// Variável global para armazenar o fluxo de transmissão da webcam ativa
let fluxoCamera = null;

// =========================================================================
// 2. LÓGICA DA WEBCAM (Ligar, Capturar e Desligar)
// =========================================================================

/**
 * Solicita permissão ao sistema operacional e transmite a câmera ao vivo na tela
 */
async function ligarCamera() {
    try {
        // Pede permissão para acessar apenas a câmera (vídeo), sem áudio
        fluxoCamera = await navigator.mediaDevices.getUserMedia({ 
            video: { width: 1280, height: 720 }, // Qualidade HD para melhor validação
            audio: false 
        });

        // Conecta o fluxo recebido diretamente à nossa tag <video>
        videoWebcam.srcObject = fluxoCamera;

        // Garante que apenas o vídeo ao vivo fique visível e os outros modos ocultos
        videoWebcam.style.display = 'block';
        canvasFoto.style.display = 'none';
        previewImagem.style.display = 'none';
        
        // Altera o estado dos botões de controle
        btnTirarFoto.disabled = false;
        btnIniciarCam.disabled = true; // Desativa para evitar ligar a câmera repetidamente
        
    } catch (erro) {
        console.error("Erro ao tentar acessar a câmera: ", erro);
        alert("Não foi possível acessar a câmera. Verifique se deu permissão de acesso no navegador.");
    }
}

/**
 * "Congela" o quadro atual do vídeo e desenha na tela usando o elemento Canvas
 */
function tirarFoto() {
    if (!fluxoCamera) return;

    // Define o tamanho real da captura igual ao tamanho que a câmera está transmitindo
    canvasFoto.width = videoWebcam.videoWidth;
    canvasFoto.height = videoWebcam.videoHeight;

    // Pega o "pincel" de desenho 2D do Canvas e desenha o frame atual do vídeo
    const contexto = canvasFoto.getContext('2d');
    contexto.drawImage(videoWebcam, 0, 0, canvasFoto.width, canvasFoto.height);

    // Oculta a transmissão de vídeo e exibe a imagem "congelada" do Canvas
    videoWebcam.style.display = 'none';
    previewImagem.style.display = 'none';
    canvasFoto.style.display = 'block';

    // Desliga a luz da webcam para economizar recursos e dar privacidade ao usuário
    desligarCamera();

    // Ativa o botão de "Validar foto" para prosseguir no fluxo
    btnValidar.disabled = false;
    
    // Libera novamente o botão de iniciar a câmera caso o operador queira refazer a foto
    btnIniciarCam.disabled = false;
}

/**
 * Para todos os feixes de transmissão da câmera de forma limpa
 */
function desligarCamera() {
    if (fluxoCamera) {
        fluxoCamera.getTracks().forEach(track => track.stop());
        fluxoCamera = null;
        videoWebcam.srcObject = null;
    }
}

// =========================================================================
// 3. LÓGICA DE ARQUIVOS (Upload local e Preview)
// =========================================================================

/**
 * Lê o arquivo de imagem do computador e exibe como preview na tela
 */
inputArquivo.addEventListener('change', function(evento) {
    const arquivo = evento.target.files[0]; // Captura o arquivo selecionado

    if (arquivo) {
        // Se a webcam estiver ativa ao fazer o upload, nós a desligamos
        desligarCamera();

        // Cria uma URL temporária e segura local para o arquivo de imagem
        const urlImagem = URL.createObjectURL(arquivo);

        // Define a URL como fonte da tag <img>
        previewImagem.src = urlImagem;

        // Oculta a câmera e o canvas, exibindo apenas a imagem carregada
        videoWebcam.style.display = 'none';
        canvasFoto.style.display = 'none';
        previewImagem.style.display = 'block';

        // Ativa o botão de validação e libera o botão de iniciar câmera caso queira voltar atrás
        btnValidar.disabled = false;
        btnIniciarCam.disabled = false;
    }
});

// =========================================================================
// 4. INTEGRAÇÃO REAL COM A SUA API JAVA
// =========================================================================

/**
 * Envia os dados encapsulados em FormData via requisição POST (fetch)
 */
async function enviarParaBackend(formData) {
    // Muda visualmente o botão para mostrar que está processando
    btnValidar.disabled = true;
    btnValidar.innerText = "Enviando...";

    try {
        // ROTA EXATA: Mapeada no seu controller Java (@RequestMapping("/api/selfies") + @PostMapping("/validar"))
        const urlAPI = 'http://localhost:8080/api/selfies/validar'; 

        const resposta = await fetch(urlAPI, {
            method: 'POST',
            body: formData
        });

        if (resposta.ok) {
            const resultado = await resposta.json();
            alert("Sucesso! Resposta do servidor: " + JSON.stringify(resultado));
        } else {
            alert("Erro no servidor: " + resposta.status);
        }

    } catch (erro) {
        console.error("Erro ao enviar foto:", erro);
        alert("Erro de conexão. Certifique-se de que a sua API Java está rodando!");
    } finally {
        // Devolve o botão ao estado normal após terminar
        btnValidar.disabled = false;
        btnValidar.innerText = "Validar foto";
    }
}

// Ouvinte de clique no botão de validar
btnValidar.addEventListener('click', async () => {
    // Cria um formulário virtual (chave-valor) para empacotar a imagem
    const dadosFormulario = new FormData();

    if (previewImagem.style.display === 'block') {
        // Caso 1: A imagem veio do UPLOAD de arquivo
        const arquivo = inputArquivo.files[0];
        
        // O nome 'foto' bate exatamente com o @RequestParam("foto") no seu Java Controller
        dadosFormulario.append('foto', arquivo); 
        enviarParaBackend(dadosFormulario);

    } else if (canvasFoto.style.display === 'block') {
        // Caso 2: A imagem foi tirada na WEBCAM (convertemos o canvas para arquivo binário Blob)
        canvasFoto.toBlob(async (blob) => {
            if (blob) {
                // O nome 'foto' bate exatamente com o @RequestParam("foto") no seu Java Controller
                dadosFormulario.append('foto', blob, 'selfie.png');
                enviarParaBackend(dadosFormulario);
            }
        }, 'image/png');
    }
});

// =========================================================================
// 5. ATIVAÇÃO DOS BOTÕES
// =========================================================================
btnIniciarCam.addEventListener('click', ligarCamera);
btnTirarFoto.addEventListener('click', tirarFoto);