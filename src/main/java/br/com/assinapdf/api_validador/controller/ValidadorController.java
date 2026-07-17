package br.com.assinapdf.api_validador.controller;

import br.com.assinapdf.api_validador.dto.RespostaValidacao; 
import br.com.assinapdf.api_validador.service.ValidadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ValidadorController {

    @Autowired
    private ValidadorService validadorService;
    
    @PostMapping("/validar")
    public ResponseEntity<RespostaValidacao> validarSelfie(@RequestParam("foto") MultipartFile foto) {
        RespostaValidacao resultado = validadorService.analisarSelfie(foto);
        
        return ResponseEntity.ok(resultado);
    }
}