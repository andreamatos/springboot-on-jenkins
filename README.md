# springboot-on-jenkins

## Configurando o jenkins com sonar e quality gate

```
---------------------------------------------------------------------
executando postgres e sonar com docker-compose para utilizar no pipeline
criar o docker-compose.yml;

  version: "3"
  services:
    pg-tasks:
      container_name: pg-tasks
      image: postgres:9.6
      ports:
        - 5433:5432
      environment:
        - POSTGRES_DB=tasks
        - POSTGRES_PASSWORD=password

    sonarqube:
        container_name: sonar
        image: sonarqube:7.9.2-community
        ports:
            - "9000:9000"
        networks:
            - sonarnet
        environment:
            - sonar.jdbc.url=jdbc:postgresql://pg-sonar:5432/sonar
        depends_on:
            - pg-sonar
        volumes:
            - sonarqube_conf:/opt/sonarqube/conf
            - sonarqube_data:/opt/sonarqube/data
            - sonarqube_extensions:/opt/sonarqube/extensions
            - sonarqube_bundled-plugins:/opt/sonarqube/lib/bundled-plugins
 
    pg-sonar:
        container_name: pg-sonar
        image: postgres:9.6
        networks:
            - sonarnet
        environment:
            - POSTGRES_USER=sonar
            - POSTGRES_PASSWORD=sonar
        volumes:
            - postgresql:/var/lib/postgresql
            - postgresql_data:/var/lib/postgresql/data
			
executar o comando docker-compose up
se houver problema com a memória executar: sudo sysctl -w vm.max_map_count=262144
---------------------------------------------------------------------
instalando jenkins;
wget -q -O - https://pkg.jenkins.io/debian-stable/jenkins.io.key | sudo apt-key add -
sudo sh -c 'echo deb http://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
sudo apt update
sudo apt install jenkins

inicializando jenkins
sudo systemctl start jenkins
sudo systemctl status jenkins

abrindo firewall
sudo ufw enable
sudo ufw allow 8080

---------------------------------------------------------------------
logando jenkins
http://172.19.0.1:8080/login?from=%2F

habilitando jenkins
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
recuperar o password e colocar na tela inicial de Unlock Jenkins

criando usuário e senha jenkins
Nome de usuário: andreamatos
Senha: 123456
Nome completo: André Amorim de Matos
Email: amatos497@gmail.com

---------------------------------------------------------------------
Instance Configuration

http://172.19.0.1:8080/

-> Logar no Jenkins
-> Manage Jenkins -> Global Tool Configuration;
	JDK -> name: JAVA_LOCAL
				 url: informar o java da maquina.
	MAVEN -> pode escolher o maven da maquina ou
						instalar a ultima versão sugerida pelo
						Jenkins.
---------------------------------------------------------------------
Criando o Pipeline
jenkins -> novo job -> escolher pipeline e incluir o nome do job -> tasks-backend -> OK
---------------------------------------------------------------------
Jenkins X JenkinsFile

Em configuraçoes; 
	Pipeline escolher -> Pipeline script from SCM
	SCM -> Git
	Repository URL -> https://github.com/andreamatos/springboot-on-jenkins.git
	Incluir suas credenciais.
	
Branch Specifier (blank for 'any')
	*/main  -> ou */master
	
Script Path -> Jenkinsfile 

Salvar
---------------------------------------------------------------------
Testando se a construçao do jenkins esta correta;

incluir no Jenkinsfile;
pipeline{
    agent any
    stages{
        stage('Build Backend'){
            steps{
                echo 'deu certo'
            }
        }
    }
}

GIT: commit -> push
Jenkins: Contruir Agora e verificar no log a mensagem 'deu certo'
---------------------------------------------------------------------
Criando Jenkinsfile para execuçao do projeto backend;

JenkinsFile;

pipeline{
    agent any
    stages{
        stage('Build Backend'){
            steps{
                sh 'mvn clean package -DskipTests=true'
            }
        }
        stage('Unit Tests'){
            steps{
                sh 'mvn test'
            }
        }
    }
}
---------------------------------------------------------------------
- Incluindo Sonar na esteira;
-> instalar plugin do sonar na opçao -> Gerenciar Jenkins -> Gerenciar Plugins

-> Gerenciar Jenkins -> Global Tool Configuration -> SonarQube Scanner -> Name = SONAR_SCANNER

no Jenkinsfile incluir o step correspondente ao sonar;
        stage('Sonar Analysis'){
            environment{
                scannerHome = tool 'SONAR_SCANNER'
            }
            steps{
                    sh "${scannerHome}/bin/sonar-scanner -e 
		    -Dsonar.projectKey=DeployBack 
		    -Dsonar.host.url=http://localhost:9000 
		    -Dsonar.login=admin 
		    -Dsonar.password=admin -Dsonar.java.binaries=target 
		    -Dsonar.coverage.exclusions=**/.mvn/**,**/src/test/**,**/model/**,**Application.java"
            }
        }

Usuario e senha Sonar: admin admin

- Criar uma credencial para o sonar;
no Jenkns entrar em;
	Dashboard -> Credentials -> System Global credentials (unrestricted);
	Kind - Secret Text
	Scope - Global(Jenkins, nodes, items, all child items, etc)
	Secret(gerar no sonar http://localhost:9000/account/security/ -> escrever jenkins, copiar e colar o token aqui)
	ID: SonarToken
	Description: Token Sonar

apos acriacao da credencial;
Dashboard -> configuração;
- SonarQube servers e em Server authentication token adicionar o token que criamos e salvar.
---------------------------------------------------------------------
Incluindo Quality Gate;

        stage('Quality Gate'){
            steps{
                sleep(5)
                timeout(time: 1, unit: 'MINUTES'){
                    waitForQualityGate abortPipeline: true
                }
            }
        }
--------------------------------------------------------------------- 
```

## Deploy da aplicaçao na imagem do servidor TomCat.

```
Para funcionar o projeto se conecta `a uma imagem do postgress, criar uma pasta src/devops e criar o seguinte
docker-compose;

docker-compose up

version: "3"
services:
  pg-tasks:
    container_name: pg-tasks
    image: postgres:9.6
    ports:
      - 5433:5432
    environment:
      - POSTGRES_DB=tasks
      - POSTGRES_PASSWORD=password

  pg-sonar:
    container_name: pg-sonar
    image: postgres:9.6
    networks:
      - sonarnet
    environment:
      - POSTGRES_USER=sonar
      - POSTGRES_PASSWORD=sonar
    volumes:
      - postgresql:/var/lib/postgresql
      - postgresql_data:/var/lib/postgresql/data

  sonarqube:
    container_name: sonar
    image: sonarqube:7.9.2-community
    ports:
      - "9000:9000"
    networks:
      - sonarnet
    environment:
      - sonar.jdbc.url=jdbc:postgresql://pg-sonar:5432/sonar
    depends_on:
      - pg-sonar
    volumes:
      - sonarqube_conf:/opt/sonarqube/conf
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions
      - sonarqube_bundled-plugins:/opt/sonarqube/lib/bundled-plugins

networks:
  sonarnet:

volumes:
  sonarqube_conf:
  sonarqube_data:
  sonarqube_extensions:
  sonarqube_bundled-plugins:
  postgresql:
  postgresql_data:
  
 o docker-compose que subira na esteira do jenkins contera a imagem da aplicaçao tasks-backend;
 version: "3"
services:
  backend:
    container_name: backend-prod
    image: back_prod:build_${BUILD_NUMBER}
    build:
      context: .
      args:
        - WAR_FILE=target/tasks-backend.war
        - CONTEXT=tasks-backend
    networks:
      - sonarnet
    ports:
      - 9998:8080
    environment:
      - DATABASE_HOST=db
      - DATABASE_PORT=5432
      - DATABASE_USER=postgres
      - DATABASE_PASSWD=passwd
      - DATABASE_UPDATE=none

networks:
  sonarnet:
  
 docker file da raiz do projeto;
 
 FROM tomcat:9.0-jdk8-openjdk
ARG WAR_FILE
ARG CONTEXT
COPY ${WAR_FILE} /usr/local/tomcat/webapps/${CONTEXT}.war

o arquivo Jenkinsfile contera o step;

        stage('Deploy Prod') {
            steps {
                sh 'docker-compose build'
                sh 'docker-compose up -d'
            }
        }
		
alterar o application.properties para;

server.port=8001

## default connection pool
spring.datasource.hikari.connectionTimeout=20000
spring.datasource.hikari.maximumPoolSize=5

## PostgreSQL
spring.datasource.url=jdbc:postgresql://${DATABASE_HOST:127.0.0.1}:${DATABASE_PORT:5433}/tasks
spring.datasource.username=${DATABASE_USER:postgres}
spring.datasource.password=${DATABASE_PASSWD:password}

#drop n create table again, good for testing, comment this in production
spring.jpa.hibernate.ddl-auto=${DATABASE_UPDATE:create}
#spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults = false
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL9Dialect

Efetuar o push no git e contruir o jenkis;

acessar a aplicacao em http://localhost:9998/tasks-backend/
```
## Referência

```
https://www.udemy.com/course/integracao-continua-jenkins/
```
