# Modo software instalado

O SIGREc pode ser entregue como software instalado sem o usuario precisar abrir manualmente o navegador.

## Modelo recomendado

1. O servidor central executa o backend Spring Boot e acessa o PostgreSQL.
2. Cada computador pode ter um atalho chamado `SIGREc`.
3. Ao abrir o atalho, o sistema inicia ou abre a interface automaticamente.

No projeto, o arquivo `SIGREc.bat` faz isso em ambiente Windows:

```text
SIGREc.bat
```

Ele usa Java 21, aponta para o banco da VM `192.168.3.200:5432/segrec` e abre `http://localhost:8080` automaticamente.

## Instalador real

Para gerar um instalador `.exe` no Windows, primeiro gere o JAR:

```powershell
mvn clean package
```

Depois use `jpackage` do JDK 21:

```powershell
jpackage --type exe --name SIGREc --input target --main-jar tfd-apac-0.1.0.jar --main-class org.springframework.boot.loader.launch.JarLauncher --java-options "-DDB_HOST=192.168.3.200" --java-options "-DDB_NAME=segrec" --java-options "-DDB_USER=jose" --java-options "-DDB_PASSWORD=Joseluiz1"
```

Assim o usuario abre o SIGREc como programa instalado. A interface continua sendo HTML interna renderizada pelo sistema, mas a entrega para o usuario final fica como software.
