## Projecto de Sistemas Distribuidos

Este projecto não functional porque há um bug em que quando um peer tenta se ligar a outro peer esse mesmo peer que 
está a tentar se ligar recebe uma conexão do localhost e acaba por so se conectar a si mesmo.

Outro bug que aconteceu no programa é que se eu tentar comparar um ip desde peer obtido por ``` InetAddress.getLocalHost() ``` 
com o mesmo ip obtido por ``` InetAddress.getByName() ``` dá false. E se eu fizer ``` InetAddress.getLocalHost().getAddress() ```
o programa retorna [-84, 17, 0, 2] quando devia retornar [127, 17, 0, 2].

### Rodar o projecto

Para correr o projecto é necessário ter o docker instalado e correr o seguinte comando:

Compilar o projecto

``` mvn clean package ```

Criar a imagem docker

``` docker build -t chat . ```

Rodar os Peers

``` docker run -d chat java -jar peer.jar <ips-dos-peers-na-network> ```
