package com.example.reportacidade.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

object LocationUtils {
    val rnCities = listOf(
        "Acari", "Afonso Bezerra", "Água Nova", "Alexandria", "Almino Afonso", "Alto do Rodrigues",
        "Angicos", "Antônio Martins", "Apodi", "Areia Branca", "Arês", "Assu", "Baía Formosa",
        "Baraúna", "Barcelona", "Bento Fernandes", "Bodó", "Bom Jesus", "Brejinho",
        "Caiçara do Norte", "Caiçara do Rio do Vento", "Caicó", "Campo Grande", "Campo Redondo",
        "Canguaretama", "Caraúbas", "Ceará-Mirim", "Cerro Corá", "Coronel Ezequiel",
        "Coronel João Pessoa", "Cruzeta", "Currais Novos", "Doutor Severiano", "Encanto",
        "Equador", "Espírito Santo", "Extremoz", "Felipe Guerra", "Fernando Pedroza", "Florânia",
        "Francisco Dantas", "Frutuoso Gomes", "Galinhos", "Goianinha", "Governador Dix-Sept Rosado",
        "Grossos", "Guamaré", "Ielmo Marinho", "Ipanguaçu", "Ipueira", "Itajá", "Itaú", "Jaçanã",
        "Jandaíra", "Janduís", "Januário Cicco (Boa Saúde)", "Japi", "Jardim de Angicos",
        "Jardim de Piranhas", "Jardim do Seridó", "João Câmara", "João Dias", "José da Penha",
        "Jucurutu", "Jundiá", "Lagoa d'Anta", "Lagoa de Pedras", "Lagoa de Velhos", "Lagoa Nova",
        "Lagoa Salgada", "Lajes", "Lajes Pintadas", "Lucrécia", "Luís Gomes", "Macaíba", "Macau",
        "Major Sales", "Marcelino Vieira", "Martins", "Maxaranguape", "Messias Targino", "Montanhas",
        "Monte Alegre", "Monte das Gameleiras", "Mossoró", "Natal", "Nísia Floresta", "Nova Cruz",
        "Olho-d'Água do Borges", "Ouro Branco", "Paraná", "Paraú", "Parazinho", "Parelhas",
        "Parnamirim", "Passa e Fica", "Passagem", "Patu", "Pau dos Ferros", "Pedra Grande",
        "Pedra Preta", "Pedro Avelino", "Pedro Velho", "Pendências", "Pilões", "Poço Branco",
        "Portalegre", "Porto do Mangue", "Pureza", "Rafael Fernandes", "Rafael Godeiro",
        "Riacho da Cruz", "Riacho de Santana", "Riachuelo", "Rio do Fogo", "Rodolfo Fernandes",
        "Ruy Barbosa", "Santa Cruz", "Santa Maria", "Santana do Matos", "Santana do Seridó",
        "Santo Antônio", "São Bento do Norte", "São Bento do Trairi", "São Fernando",
        "São Francisco do Oeste", "São Gonçalo do Amarante", "São João do Sabugi",
        "São José de Mipibu", "São José do Campestre", "São José do Seridó", "São Miguel",
        "São Miguel do Gostoso", "São Paulo do Potengi", "São Pedro", "São Rafael", "São Tomé",
        "São Vicente", "Senador Elói de Souza", "Senador Georgino Avelino", "Serra de São Bento",
        "Serra do Mel", "Serra Negra do Norte", "Serrinha", "Serrinha dos Pintos", "Severiano Melo",
        "Sítio Novo", "Taboleiro Grande", "Taipu", "Tangará", "Tenente Ananias",
        "Tenente Laurentino Cruz", "Tibau", "Tibau do Sul", "Timbaúba dos Batistas", "Touros",
        "Triunfo Potiguar", "Umarizal", "Upanema", "Várzea", "Venha-Ver", "Vera Cruz", "Viçosa",
        "Vila Flor"
    )

    val neighborhoodMap = mapOf(
        "Natal" to listOf(
            "Candelária", "Capim Macio", "Lagoa Nova", "Neópolis", "Nova Descoberta", "Pitimbu", "Ponta Negra",
            "Alecrim", "Areia Preta", "Barro Vermelho", "Cidade Alta", "Lagoa Seca", "Mãe Luíza", "Petrópolis",
            "Praia do Meio", "Ribeira", "Rocas", "Santos Reis", "Tirol", "Igapó", "Lagoa Azul",
            "Nossa Senhora da Apresentação", "Pajuçara", "Potengi", "Redinha", "Salinas", "Bom Pastor",
            "Cidade da Esperança", "Cidade Nova", "Dix-Sept Rosado", "Felipe Camarão", "Guarapes", "Nordeste",
            "Nossa Senhora de Nazaré", "Planalto", "Quintas"
        ),
        "Mossoró" to listOf(
            "Centro", "Doze Anos", "Paredões", "Bom Jardim", "Santo Antônio", "Barrocas", "Bom Pastor",
            "Santa Helena", "Estrada da Raiz", "Independência", "Santa Júlia", "José Agripino", "Alto de São Manoel",
            "Ilha de Santa Luzia", "Planalto 13 de Maio", "Dom Jaime Câmara", "Costa e Silva", "Pintos", "Rincão",
            "Sumaré", "Vingt Rosado", "Liberdade I e II", "Alameda dos Cajueiros", "Walfredo Gurgel", "Teimosos",
            "Cohab", "Ulrick Graff", "Abolição (I, II, III, IV e V)", "Nova Betânia", "Aeroporto (I e II)",
            "Santa Delmira", "Redenção", "Nova Mossoró", "Três Vinténs", "Monsenhor Américo", "Bela Vista",
            "Belo Horizonte", "Boa Vista", "Dix-Sept Rosado", "Lagoa do Mato", "Alagados", "Itapetinga", "Carnaubal"
        ),
        "Parnamirim" to listOf(
            "Bela Parnamirim", "Boa Esperança", "Cajupiranga", "Centro", "Cohabinal", "Emaús", "Encanto Verde",
            "Jardim Planalto", "Liberdade", "Monte Castelo", "Nova Esperança", "Nova Parnamirim", "Parque das Árvores",
            "Parque das Nações", "Parque de Exposições", "Parque do Jiqui", "Passagem de Areia", "Rosa dos Ventos",
            "Santa Tereza", "Santos Reis", "Vale do Sol", "Vida Nova", "Cotovelo (Litoral)", "Pirangi do Norte (Litoral)",
            "Pium (Litoral)"
        ),
        "Caicó" to listOf(
            "Centro", "Acampamento", "Penedo", "Nova Descoberta", "Castelo Branco", "Canuto e Filhos", "Vila Altiva",
            "Jardim Satélite", "Loteamento Vila do Príncipe", "Loteamento Seridó", "Loteamento Cidade Alta",
            "Boa Passagem", "Vila do Príncipe", "Recreio", "Darci Fonseca", "Alto da Boa Vista", "Loteamento Gabriel Diniz",
            "Loteamento Stella Maris", "Barra Nova", "Batalhão", "Paulo VI", "João XXIII", "Walfredo Gurgel",
            "Adjunto Dias", "Frei Damião", "Casas Populares", "Novo Horizonte", "Paraíba", "Soledade"
        ),
        "São Gonçalo do Amarante" to listOf(
            "Centro", "Amarante", "Jardim Lola", "Santo Antônio do Potengi", "Golandim", "Regomoleiro", "Guanduba",
            "Novo Amarante", "Serrinha", "Ouro Verde", "Barreiros", "Santa Terezinha"
        ),
        "Macaíba" to listOf(
            "Centro", "Auta de Souza", "Campo das Mangueiras", "Ferrareis", "Vila Elã", "Aliança", "Morada da Fé", "Baixa Verde"
        ),
        "Assu" to listOf(
            "Centro", "Alto do Rosário", "Bela Vista", "Dom Elizeu", "Frutilândia", "Novo Horizonte", "Quinta do Farol", "Vertentes"
        ),
        "Ceará-Mirim" to listOf(
            "Centro", "Brogodó", "Coqueiros", "Nova Descoberta", "Passagem de Areia", "Planalto", "Santa Águeda"
        ),
        "Currais Novos" to listOf(
            "Centro", "Dr. José Bezerra", "Gilberto Pinheiro", "JK", "Paizinho Maria", "Parque das Nações", "Promorar", "Radir Azul", "Santa Maria"
        ),
        "Nova Cruz" to listOf(
            "Centro", "Alto de Santa Luzia", "Coréia", "Frei Damião", "Pascoal", "Salgado", "São Sebastião"
        ),
        "Pau dos Ferros" to listOf(
            "Centro", "Carvão", "João XXIII", "Manoel Domingos", "Paraíba", "Princesa do Oeste", "Riacho do Meio", "São Benedito"
        ),
        "Santa Cruz" to listOf(
            "Centro", "Conjunto Alonço Bezerra", "Conjunto Cônego Monte", "DNER", "Maracujá", "Paraíso"
        )
    )
}

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onSuccess: (LocationData) -> Unit, onError: () -> Unit) {
        val cancelToken = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelToken.token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onSuccess(LocationData(location.latitude, location.longitude))
                } else {
                    onError()
                }
            }
            .addOnFailureListener { onError() }
    }
}
