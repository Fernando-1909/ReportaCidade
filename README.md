# ReportaCidade

O ReportaCidade é uma plataforma móvel desenvolvida para integrar o cidadão à gestão urbana. O aplicativo permite o registro de ocorrências civis, como problemas de infraestrutura, iluminação e saneamento, utilizando geolocalização e suporte a mídias para facilitar a identificação e resolução por parte dos órgãos competentes.

## Funcionalidades Principais

- Autenticação de Usuários: Sistema de login e cadastro de novos usuários.
- Mapa Interativo: Visualização de ocorrências em tempo real com integração ao Google Maps e MapLibre.
- Relatórios Geolocalizados: Registro de problemas com captura automática de coordenadas GPS.
- Gestão de Mídias: Suporte para anexar fotografias aos relatos para comprovação visual.
- Persistência de Dados: Integração com banco de dados para armazenamento de histórico de notificações e relatos.

## Tecnologias e Dependências

O projeto foi construído utilizando a linguagem Kotlin e segue os padrões de arquitetura modernos para Android.

- Linguagem: Kotlin 2.2.10
- Interface: XML com Material Design 3
- Backend e Banco de Dados: Firebase (Auth, Firestore, Storage)
- Mapas e Localização: 
    - Google Play Services Maps
    - Google Play Services Location
    - MapLibre SDK
- Processamento Assíncrono: Kotlin Coroutines
- Serialização de Dados: GSON

## Configuração do Ambiente

Para compilar e executar o projeto, siga as etapas abaixo:

1. Clone o repositório:
   git clone https://github.com/Fernando-1909/ReportaCidade.git

2. Configuração de APIs do Google:
   - Ative o Maps SDK for Android no Google Cloud Console.
   - Insira sua API Key no arquivo AndroidManifest.xml no campo com.google.android.geo.API_KEY.

3. Configuração do Firebase:
   - Crie um projeto no console do Firebase.
   - Adicione o arquivo google-services.json na pasta /app do projeto.

4. Build:
   - Sincronize o Gradle através do Android Studio e execute o projeto em um dispositivo com Android 7.0 (API 24) ou superior.

## Estrutura de Arquivos Relevantes

- LoginActivity.kt: Gerencia o fluxo de entrada e validação de usuários.
- MainActivity.kt: Tela principal que abriga o mapa e os recursos de navegação.
- AuthRepository: Interface de abstração para os serviços de autenticação.
- MockAuthRepositoryImpl: Implementação de repositório utilizada para testes e validação de fluxo.

## Créditos e Equipe

Este projeto foi desenvolvido sob rigor acadêmico e técnico pelos seguintes integrantes:

- Orientador:
    - Raul Benites Paradeda
- Programadores: 
    - Fernando Macedo da Costa
    - José Inácio Mendes Ferreira

---
Repositório oficial: https://github.com/Fernando-1909/ReportaCidade
