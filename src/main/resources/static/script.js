const DOM = Object.freeze({
    video: document.getElementById('webcam'),
    canvas:document.getElementById('foto-capturada'),
    preview:document.getElementById('preview-imagem'),
    btnIniciar: document.getElementById('btn-iniciar-cam'),
    btnTirar: document.getElementById('btn-tirar-foto'),
    btnValidar: document.getElementById('btn-validar'),
    inputArquivo: document.getElementById('input-arquivo')
});
const AppState = {
    cameraStream: null,
    status: 'IDLE',
    transitionTo(novoEstado){
        this.status=novoEstado;
        this.aplicarRegrasDoEstado();
    },
    aplicarRegrasDoEstado(){
        switch(this.status){
            case 'IDLE':
                DOM.btnIniciar.disabled = false;
                DOM.btnTirar.disabled = true;
                DOM.btnValidar.disabled = true
                break;
            case 'STREAMING':
                DOM.btnIniciar.disabled =true;
                DOM.btnTirar.disabled = false;
                DOM.btnValidar.disabled = true;
                break;
            case 'CAPTURED':
                DOM.btnIniciar.disabled =false;
                DOM.btnTirar.disabled = true;
                DOM.btnValidar.disabled = false;
                break;
            case 'UPLOADING':
                DOM.btnIniciar.disabled =true;
                DOM.btnTirar.disabled =true ;
                DOM.btnValidar.disabled = true;
                break;
            
        }
    }
};
function verificarIntegridadeDOM(){
    const elementosFaltando = Object.entries(DOM)
    .filter(([chave, valor]) =>!valor)
    .map(([chave])=>chave);
            if(elementosFaltando.length > 0){
                console.error(`Erro de inicialização: IDs não encontrados:${elementosFaltando.join(`, `)}`)
            }else{
                AppState.transitionTo('IDLE');
                console.log("DOM mapeado e AppState iniciado com sucesso.")
            }
        }
verificarIntegridadeDOM();
        async function ligarCam(){
            try{
                const stream =await navigator.mediaDevices.getUserMedia({
                    video:{ width: 1280, height: 720},
                    audio: false
                });
                AppState.cameraStream = stream;
                DOM.video.srcObject = stream;
                DOM.video.style.display='block';
                    if(DOM.preview)DOM.preview.style.display = 'none';
                AppState.transitionTo('STREAMING');
            console.log("Câmera inciada.");
            }catch(erro){
            console.error("Erro ao acessar câmera", erro);
                alert("Não foi possivel acessar a câmera. Verifique as permissões do seu navegador e seu dispositivo");
            }
        }
DOM.btnIniciar.addEventListener('click', ligarCam);
        function tirarFoto(){
            DOM.canvas.width = DOM.video.videoWidth;
            DOM.canvas.height = DOM.video.videoHeight;
                const contexto = DOM.canvas.getContext('2d');
                    contexto.drawImage(DOM.video, 0, 0, DOM.canvas.width, DOM.canvas.height);
                    const fotoBase64 = DOM.canvas.toDataURL('image/jpeg');
                        if(DOM.preview){
                            DOM.preview.src = fotoBase64;
                            DOM.preview.style.display = 'block'
                        }
                DOM.video.style.display = 'none';
                        if(AppState.cameraStream){
                            AppState.cameraStream.getTracks().forEach(track=>track.stop());
                            AppState.transitionTo('CAPTURED');
            console.log("Foto capturada com sucesso")
                        }
        }
DOM.btnTirar.addEventListener('click', tirarFoto)
        function manipularUploadArquivo(evento){
            const arquivo = evento.target.files[0];
                if(!arquivo) 
                    return;
                if(!arquivo.type.startsWith('image/')){
                    alert("Por favor, selecione apenas arquivos de imagem.");
                    DOM.inputArquivo.value='';
                    return; } 
                const leitor = new FileReader();
                leitor.onload = function(e){
                    const fotoBase64 = e.target.result;
                        if(DOM.preview){
                            DOM.preview.src = fotoBase64;
                            DOM.preview.style.display = 'block';
                        }
                DOM.video.style.display = 'none';
        if(AppState.cameraStream){
            AppState.cameraStream.getTracks().forEach(track=> track.stop());
        }
        AppState.transitionTo('CAPTURED');
                console.log("Arquivo local carregado com sucesso");
                };
            leitor.readAsDataURL(arquivo);
        }
DOM.inputArquivo.addEventListener('change', manipularUploadArquivo);
        async function enviarFoto(){
            const imagemBase64 = DOM.preview ? DOM.preview.src : null;
                if(!imagemBase64 || imagemBase64.startsWith('http://') || imagemBase64.startsWith('https://')){
                    alert("Por favor, tire uma foto ou selecione um arquivo válido");
                    return;
                }
        AppState.transitionTo('UPLOADING');
                console.log("Imagem sendo enviada para análise");
                    try{
                        const respostaPrevia = await fetch(imagemBase64);
                        const blobImagem = await respostaPrevia.blob();
                        const formData = new FormData();
                            formData.append('foto', blobImagem, 'selfie.jpg');
                        const resposta = await fetch('http://localhost:8080/api/validar',{
                            method:'POST',
                            body: formData
                        });
                        if(!resposta.ok){
                            throw new Error(`Erro no servidor: ${resposta.status}`);
                        }
                        const resultado=await resposta.json();
                        console.log("Resposta", resultado);
                        alert(`Resultado: ${resultado.mensagem}`);

                        restaurarEstadoInicial();
                    }catch(erro){
                        console.error("Falha ao enviar requisição.");
                        alert(`Não foi possível realizar a validção: ${erro.message}`)
                AppState.transitionTo('CAPTURED');
             }

        }
        function restaurarEstadoInicial(){
            if(DOM.preview){
                DOM.preview.src = '';
                DOM.preview.style.display='none';
            }
            DOM.video.style.display='none';
            DOM.inputArquivo.value='';
            AppState.transitionTo('IDLE');
        }
DOM.btnValidar.addEventListener('click', enviarFoto);