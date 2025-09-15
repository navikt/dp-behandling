package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.uuid.UUIDv7
import java.util.UUID

object OpplysningsTyper {
    val FødselsdatoId = Opplysningstype.Id(UUID.fromString("0194881f-940b-76ff-acf5-ba7bcb367233"), Dato)
    val AldersgrenseId = Opplysningstype.Id(UUID.fromString("0194881f-940b-76ff-acf5-ba7bcb367234"), Heltall)
    val SisteMånedId = Opplysningstype.Id(UUID.fromString("0194881f-940b-76ff-acf5-ba7bcb367235"), Dato)
    val SisteDagIMånedId = Opplysningstype.Id(UUID.fromString("0194881f-940b-76ff-acf5-ba7bcb367236"), Dato)
    val KravTilAlderId = Opplysningstype.Id(UUID.fromString("0194881f-940b-76ff-acf5-ba7bcb367237"), Boolsk)
    val AntallÅrI36MånederId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10ca9"), Desimaltall)
    val FaktorForMaksimaltMuligGrunnlagId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10ca7"), Desimaltall)
    val SeksGangerGrunnbeløpId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10ca8"), Penger)
    val GrunnbeløpForGrunnlagId = Opplysningstype.Id(UUID.fromString("0194881f-940f-7af9-9387-052e028b29ee"), Penger)
    val TellendeInntektId = Opplysningstype.Id(UUID.fromString("0194881f-940f-7af9-9387-052e028b29ed"), InntektDataType)
    val OppjustertInntektId = Opplysningstype.Id(UUID.fromString("0194881f-940f-7af9-9387-052e028b29ec"), InntektDataType)
    val UtbetaltArbeidsinntektPeriode1Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cad"), Penger)
    val UtbetaltArbeidsinntektPeriode2Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cae"), Penger)
    val UtbetaltArbeidsinntektPeriode3Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10caf"), Penger)
    val Inntektperiode1Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb0"), Penger)
    val Inntektperiode2Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb1"), Penger)
    val Inntektperiode3Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb2"), Penger)
    val UavkortetGrunnlagSiste12MndId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cbe"), Penger)
    val UavkortetGrunnlagSiste36MndId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cbf"), Penger)
    val AvkortetInntektperiode1Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb3"), Penger)
    val AvkortetInntektperiode2Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb4"), Penger)
    val AvkortetInntektperiode3Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb5"), Penger)
    val GrunnlagSiste12MndId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10caa"), Penger)
    val InntektSiste36MånederId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cab"), Penger)
    val GjennomsnittligArbeidsinntektSiste36MånederId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cac"), Penger)
    val UavrundetGrunnlagId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cbb"), Penger)
    val BruktBeregningsregelId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cba"), Tekst)
    val GrunnlagVedOrdinæreDagpengerId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cbc"), Penger)
    val GrunnlagId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cbd"), Penger)
    val HarAvkortetGrunnlagetIPeriode1Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb6"), Boolsk)
    val HarAvkortetGrunnlagetIPeriode2Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb7"), Boolsk)
    val HarAvkortetGrunnlagetIPeriode3Id = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb8"), Boolsk)
    val HarAvkortetGrunnlagId = Opplysningstype.Id(UUID.fromString("0194881f-9410-7481-b263-4606fdd10cb9"), Boolsk)
    val BarnId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a23b"), BarnDatatype)
    val AntallBarnSomGirRettTilBarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a23c"), Heltall)
    val BarnetillegDekningsgradId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a23e"), Desimaltall)
    val DagsatsUtenBarnetilleggFørSamordningId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a23f"), Penger)
    val AvrundetDagsatsUtenBarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a241"), Penger)
    val BarnetilleggetsStørrelsePerDagId =
        Opplysningstype.Id(
            UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a23d"),
            Penger,
        )
    val BarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a244"), Penger)
    val DagsatsMedBarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a245"), Penger)
    val AvrundetUkessatsMedBarnetilleggFørSmordningId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a240"), Penger)
    val NittiProsentId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a246"), Desimaltall)
    val AntallArbeidsdagerPerÅrId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a247"), Heltall)
    val MaksGrunnlagId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a248"), Penger)
    val MaksSatsId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a24a"), Penger)
    val AvrundetMaksSatsId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a24b"), Penger)
    val beløpOverMaksId =
        Opplysningstype.Id(
            UUID.fromString(
                "0194881f-9428-74d5-b160-f63a4c61a242",
            ),
            Penger,
        )
    val DagsatsEtterNittiProsentId =
        Opplysningstype.Id(
            UUID.fromString(
                "0194881f-9428-74d5-b160-f63a4c61a243",
            ),
            Penger,
        )
    val SamordnetDagsatsMedBarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a24d"), Penger)
    val DagsatsEtterSamordningMedBarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a24f"), Penger)
    val HarSamordnetId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a250"), Boolsk)
    val ArbeidsdagerPerUkeId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a249"), Heltall)
    val UkessatsId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a24e"), Penger)
    val HarBarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a24c"), Boolsk)
    val KortPeriodeId = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f1544a"), Heltall)
    val LangPeriodeId = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f1544b"), Heltall)
    val Terskel12Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f1544c"), Penger)
    val Terskel36Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f1544d"), Penger)
    val DivisiorId = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f1544e"), Desimaltall)
    val TerskelFaktor12Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f1544f"), Desimaltall)
    val TerskelFaktor36Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15450"), Desimaltall)
    val InntektSnittSiste36Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15451"), Penger)
    val Overterskel12Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15454"), Boolsk)
    val Overterskel36Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15455"), Boolsk)
    val Stønadsuker12Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15452"), Heltall)
    val Stønadsuker36Id = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15453"), Heltall)
    val AntallStønadsukerId = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15456"), Heltall)
    val IngenOrdinærPeriodeId = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15458"), Heltall)
    val OrdinærPeriodeId = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15459"), Heltall)
    val DagerIUkaId = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15449"), Heltall)
    val GjenståendeStønadsdagerId = Opplysningstype.Id(UUID.fromString("0194881f-943d-77a7-969c-147999f15457"), Heltall)
    val AntallDagsatsForEgenandelId = Opplysningstype.Id(UUID.fromString("0194881f-943f-78d9-b874-00a4944c54f0"), Desimaltall)
    val EgenandelId = Opplysningstype.Id(UUID.fromString("0194881f-943f-78d9-b874-00a4944c54ef"), Penger)
    val TreGangerDagsatsId = Opplysningstype.Id(UUID.fromString("019523aa-7941-7dd2-8c43-0644d7b43f57"), Penger)
    val IngenEgenandelId = Opplysningstype.Id(UUID.fromString("019523aa-980d-7805-b6ed-d701e7827998"), Penger)
    val IkkeFulleYtelserId = Opplysningstype.Id(UUID.fromString("0194881f-943f-78d9-b874-00a4944c54f1"), Boolsk)
    val MinsteinntektEllerVernepliktId = Opplysningstype.Id(UUID.fromString("0194881f-9440-7e1c-9ec4-0f20650bc0cd"), Boolsk)
    val DagensDatoId = Opplysningstype.Id(UUID.fromString("0194881f-9440-7e1c-9ec4-0f20650bc0cf"), Dato)
    val EttBeregnetVirkningstidspunktId = Opplysningstype.Id(UUID.fromString("0194881f-9440-7e1c-9ec4-0f20650bc0ce"), Dato)
    val KravPåDagpengerId = Opplysningstype.Id(UUID.fromString("0194881f-9440-7e1c-9ec4-0f20650bc0cc"), Boolsk)
    val MaksPeriodeLengdeId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04246"), Heltall)
    val FørsteMånedAvOpptjeningsperiodeId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04247"), Dato)
    val InntektsopplysningerId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04244"), InntektDataType)
    val BruttoArbeidsinntektId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04245"), InntektDataType)
    val GrunnbeløpId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04243"), Penger)
    val InntektSiste12MndId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04241"), Penger)
    val TerskelFaktor12MndId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f0423f"), Desimaltall)
    val Inntektskrav12mndId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04248"), Penger)
    val Over12mndTerskelId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f0424a"), Boolsk)
    val InntektSiste36MndId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04242"), Penger)
    val TerskelFaktor36MndId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04240"), Desimaltall)
    val Inntektskrav36MndId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f04249"), Penger)
    val Over36mndTerskelId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f0424b"), Boolsk)
    val KravTilMinsteinntektId = Opplysningstype.Id(UUID.fromString("0194881f-9413-77ce-92ec-d29700f0424c"), Boolsk)
    val OppholdINorgeId = Opplysningstype.Id(UUID.fromString("0194881f-9443-72b4-8b30-5f6cdb24d549"), Boolsk)
    val UnntakForOppholdId = Opplysningstype.Id(UUID.fromString("0194881f-9443-72b4-8b30-5f6cdb24d54a"), Boolsk)
    val OppfyllerKravetTilOppholdId = Opplysningstype.Id(UUID.fromString("0194881f-9443-72b4-8b30-5f6cdb24d54b"), Boolsk)
    val MedlemFolketrygdenId = Opplysningstype.Id(UUID.fromString("0194881f-9443-72b4-8b30-5f6cdb24d54c"), Boolsk)
    val OppfyllerMedlemskapId = Opplysningstype.Id(UUID.fromString("0194881f-9443-72b4-8b30-5f6cdb24d54d"), Boolsk)
    val OppfyllerKravetOppholdId = Opplysningstype.Id(UUID.fromString("0194881f-9443-72b4-8b30-5f6cdb24d54e"), Boolsk)
    val BostedslandId = Opplysningstype.Id(UUID.fromString("0196ab10-0cff-7301-99d6-65be50a50201"), Boolsk)
    val PliktigRapporteringsfristId = Opplysningstype.Id(UUID.fromString("0194881f-9414-7823-8d29-0e25b7feb7ce"), Dato)
    val ArbeidsgiversRapporteringsfristId = Opplysningstype.Id(UUID.fromString("0194881f-9414-7823-8d29-0e25b7feb7cf"), Dato)
    val SisteAvsluttendeKalenderMånedId = Opplysningstype.Id(UUID.fromString("0194881f-9414-7823-8d29-0e25b7feb7d0"), Dato)
    val KanJobbeDeltidId = Opplysningstype.Id(UUID.fromString("0194881f-9441-7d1b-a06a-6727543a141e"), Boolsk)
    val GodkjentDeltidssøkerId = Opplysningstype.Id(UUID.fromString("0194881f-9441-7d1b-a06a-6727543a141f"), Boolsk)
    val KanJobbeHvorSomHelstId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877bd9"), Boolsk)
    val GodkjentLokalArbeidssøker = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877bda"), Boolsk)
    val ErArbeidsførId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877bdc"), Boolsk)
    val GodkjentArbeidsuførId = Opplysningstype.Id(UUID.fromString("0194929e-2036-7ac1-ada3-5cbe05a83f08"), Boolsk)
    val VilligTilEthvertArbeidId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877bde"), Boolsk)
    val OppfyllerKravTilArbeidssøkerId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877bd8"), Boolsk)
    val OppfyllerKravTilMobilitetId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877bdb"), Boolsk)
    val OppfyllerKravTilArbeidsførId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877bdd"), Boolsk)
    val OppfyllerKravetTilEthvertArbeidId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877bdf"), Boolsk)
    val RegistrertSomArbeidssøkerId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877be0"), Boolsk)
    val UnntakForArbeidssøkerId = Opplysningstype.Id(UUID.fromString("019874db-9555-70e6-a9c3-e3dd785d7118"), Boolsk)
    val OppyllerKravTilRegistrertArbeidssøkerId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877be1"), Boolsk)
    val KravTilArbeidssøkerId = Opplysningstype.Id(UUID.fromString("0194881f-9442-707b-a6ee-e96c06877be2"), Boolsk)
    val ErReellArbeidssøkerVurdertId = Opplysningstype.Id(UUID.fromString("0194ff86-a035-7eb0-9c60-21899f7cc0c1"), Boolsk)
    val SkalVernepliktVurderesId = Opplysningstype.Id(UUID.fromString("01980cf4-9010-7bcf-b578-ca5a825d64ef"), Boolsk)
    val OrdinærId = Opplysningstype.Id(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d85"), Boolsk)
    val PermittertId = Opplysningstype.Id(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d86"), Boolsk)
    val LønnsgarantiId = Opplysningstype.Id(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d87"), Boolsk)
    val PermittertFiskeforedlingId = Opplysningstype.Id(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d88"), Boolsk)
    val HarRettTilOrdinærId = Opplysningstype.Id(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d8a"), Boolsk)
    val IngenArbeidId = Opplysningstype.Id(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d89"), Boolsk)
    val RettighetstypeId = Opplysningstype.Id(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d8b"), Boolsk)
    val oppgittAndreYtelserUtenforNavId = Opplysningstype.Id(UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e0"), Boolsk)
    val mottarPensjonFraEnOffentligTjenestepensjonsordningId =
        Opplysningstype.Id(
            UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e1"),
            Boolsk,
        )
    val redusertUførepensjonId = Opplysningstype.Id(UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e2"), Boolsk)
    val mottarVartpengerId = Opplysningstype.Id(UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e3"), Boolsk)
    val mottarVentelønnId = Opplysningstype.Id(UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e4"), Boolsk)
    val mottarEtterlønnId = Opplysningstype.Id(UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e5"), Boolsk)
    val mottarGarantilottId = Opplysningstype.Id(UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e6"), Boolsk)
    val andreØkonomiskeYtelserId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a2a"), Boolsk)
    val beløpFraOffentligTjenestepensjonsordningId = Opplysningstype.Id(UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e7"), Penger)
    val beløpOffentligPensjonsordningId = Opplysningstype.Id(UUID.fromString("0194881f-942e-7cb0-aa59-05ea449d88e8"), Penger)
    val beløpVartpengerId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a26"), Penger)
    val beløpVentelønnId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a27"), Penger)
    val beløpEtterlønnId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a28"), Penger)
    val beløpGarantilottId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a29"), Penger)
    val sumYtelserUtenforFolketrygdenId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a2e"), Penger)
    val terskelVedSamordningId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a2b"), Desimaltall)
    val nedreGrenseForSamordningId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a2c"), Penger)
    val samordnetUkessatsUtenBarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a2f"), Penger)
    val minsteMuligeUkessatsSomKanBrukesId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a30"), Penger)
    val samordnetUkessatsUtenforFolketrygdenId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a31"), Penger)
    val samordnetUkessatsMedFolketrygdenId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a32"), Penger)
    val dagsatsSamordnetUtenforFolketrygdenId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a33"), Penger)
    val skalSamordnesMedYtelserUtenforFolketrygdenId = Opplysningstype.Id(UUID.fromString("0194881f-942f-7bde-ab16-68ffd19e9a2d"), Boolsk)
    val sykepengerId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45cd"), Boolsk)
    val pleiepengerId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45ce"), Boolsk)
    val omsorgspengerId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45cf"), Boolsk)
    val opplæringspengerId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d0"), Boolsk)
    val foreldrepengerId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d2"), Boolsk)
    val svangerskapspengerId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d3"), Boolsk)
    val uføreId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d1"), Boolsk)
    val antallTimerArbeidstidenSkalSamordnesMotId = Opplysningstype.Id(UUID.fromString("0194881f-9434-79e8-a64d-1a23cc5d86ed"), Desimaltall)
    val samordnetFastsattArbeidstidId = Opplysningstype.Id(UUID.fromString("0194881f-9434-79e8-a64d-1a23cc5d86ee"), Desimaltall)
    val sykepengerDagsatsId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d4"), Penger)
    val pleiepengerDagsatsId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d5"), Penger)
    val omsorgspengerDagsatsId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d6"), Penger)
    val opplæringspengerDagsatsId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d7"), Penger)
    val uføreDagsatsId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d8"), Penger)
    val svangerskapspengerDagsatsId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45da"), Penger)
    val foreldrepengerDagsatsId = Opplysningstype.Id(UUID.fromString("0194881f-9433-70e9-a85b-c246150c45d9"), Penger)
    val sumAndreYtelserId = Opplysningstype.Id(UUID.fromString("0194881f-9434-79e8-a64d-1a23cc5d86e9"), Penger)
    val samordnetDagsatsUtenBarnetilleggId = Opplysningstype.Id(UUID.fromString("0194881f-9434-79e8-a64d-1a23cc5d86eb"), Penger)
    val samordnetDagsatsErNegativEller0Id = Opplysningstype.Id(UUID.fromString("0194881f-9434-79e8-a64d-1a23cc5d86ec"), Boolsk)
    val utfallEtterSamordningId = Opplysningstype.Id(UUID.fromString("0194881f-9434-79e8-a64d-1a23cc5d86ef"), Boolsk)
    val skalSamordnesId = Opplysningstype.Id(UUID.fromString("0194881f-9434-79e8-a64d-1a23cc5d86ea"), Boolsk)
    val deltarStreikEllerLockoutId = Opplysningstype.Id(UUID.fromString("0194881f-91df-746a-a8ac-4a6b2b30685d"), Boolsk)
    val ledigVedSammeBedriftOgPåvirketAvUtfalletId = Opplysningstype.Id(UUID.fromString("0194881f-91df-746a-a8ac-4a6b2b30685e"), Boolsk)
    val ikkePåvirketAvStreikEllerLockoutId = Opplysningstype.Id(UUID.fromString("0194881f-91df-746a-a8ac-4a6b2b30685f"), Boolsk)
    val søknadId = Opplysningstype.Id(UUID.fromString("0194881f-91d1-7df2-ba1d-4533f37fcc77"), Tekst)
    val søknadsdatoId = Opplysningstype.Id(UUID.fromString("0194881f-91d1-7df2-ba1d-4533f37fcc73"), Dato)
    val ønskerDagpengerFraDatoId = Opplysningstype.Id(UUID.fromString("0194881f-91d1-7df2-ba1d-4533f37fcc74"), Dato)
    val søknadstidspunktId = Opplysningstype.Id(UUID.fromString("0194881f-91d1-7df2-ba1d-4533f37fcc75"), Dato)
    val prøvingsdatoId = Opplysningstype.Id(UUID.fromString("0194881f-91d1-7df2-ba1d-4533f37fcc76"), Dato)
    val ønsketArbeidstidId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a75f"), Desimaltall)
    val harTaptArbeidId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a75d"), Boolsk)
    val kravPåLønnId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a75e"), Boolsk)
    val ikkeKravPåLønnFraTidligereArbeidsgiverId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a760"), Boolsk)
    val kravTilTapAvArbeidsinntektId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a761"), Boolsk)
    val kravTilProsentvisTapAvArbeidstidId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a762"), Desimaltall)
    val ordinærtKravTilTaptArbeidstidId = Opplysningstype.Id(UUID.fromString("019522d6-846d-7173-a892-67f10016d8d2"), Desimaltall)
    val beregningsregelArbeidstidSiste6MånederId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a764"), Boolsk)
    val beregningsregelArbeidstidSiste12MånederId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a765"), Boolsk)
    val beregeningsregelArbeidstidSiste36MånederId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a766"), Boolsk)
    val beregnetVanligArbeidstidPerUkeFørTapId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a767"), Desimaltall)
    val fastsattVanligArbeidstidEtterOrdinærEllerVernepliktId =
        Opplysningstype.Id(
            UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a76c"),
            Desimaltall,
        )
    val nyArbeidstidPerUkeId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a76b"), Desimaltall)
    val maksimalVanligArbeidstidId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a768"), Desimaltall)
    val minimumVanligArbeidstidId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a769"), Desimaltall)
    val fastsattArbeidstidPerUkeFørTapId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a76a"), Desimaltall)
    val villigTilMinimumArbeidstidId =
        Opplysningstype.Id(
            UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a76d"),
            Boolsk,
        )
    val tapAvArbeidstidErMinstTerskelId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a76e"), Boolsk)
    val beregningsregelTaptArbeidstidId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a763"), Boolsk)
    val kravTilTapAvArbeidsinntektOgArbeidstidId = Opplysningstype.Id(UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a76f"), Boolsk)
    val tarUtdanningEllerOpplæringId = Opplysningstype.Id(UUID.fromString("0194881f-9445-734c-a7ee-045edf29b522"), Boolsk)
    val deltarIArbeidsmarkedstiltakId = Opplysningstype.Id(UUID.fromString("0194881f-9445-734c-a7ee-045edf29b527"), Boolsk)
    val deltarIOpplæringForInnvandrereId = Opplysningstype.Id(UUID.fromString("0194881f-9445-734c-a7ee-045edf29b528"), Boolsk)
    val grunnskoleopplæringId =
        Opplysningstype.Id(
            UUID.fromString(
                "0194881f-9445-734c-a7ee-045edf29b529",
            ),
            Boolsk,
        )
    val deltarIHøyereYrkesfagligUtdanningId = Opplysningstype.Id(UUID.fromString("0194881f-9445-734c-a7ee-045edf29b52a"), Boolsk)
    val deltarIHøyereUtdanningId = Opplysningstype.Id(UUID.fromString("0194881f-9445-734c-a7ee-045edf29b52b"), Boolsk)
    val deltarPåKursMvId = Opplysningstype.Id(UUID.fromString("0194881f-9445-734c-a7ee-045edf29b52c"), Boolsk)
    val godkjentUnntakForUtdanningId = Opplysningstype.Id(UUID.fromString("0194881f-9445-734c-a7ee-045edf29b523"), Boolsk)
    val svartJaPåUtdanningId =
        Opplysningstype.Id(
            UUID.fromString("0194881f-9445-734c-a7ee-045edf29b524"),
            Boolsk,
        )
    val svartNeiPåUtdanningId =
        Opplysningstype.Id(
            UUID.fromString("0194881f-9445-734c-a7ee-045edf29b525"),
            Boolsk,
        )
    val oppfyllerKravetPåUnntakForUtdanningId =
        Opplysningstype.Id(
            UUID.fromString("0194881f-9445-734c-a7ee-045edf29b526"),
            Boolsk,
        )
    val kravTilUtdanningEllerOpplæringId = Opplysningstype.Id(UUID.fromString("0194881f-9445-734c-a7ee-045edf29b52d"), Boolsk)
    val brukerErUtestengtFraDagpengerId = Opplysningstype.Id(UUID.fromString("0194881f-9447-7e36-a569-3e9f42bff9f6"), Boolsk)
    val oppfyllerKravTilIkkeUtestengtId = Opplysningstype.Id(UUID.fromString("0194881f-9447-7e36-a569-3e9f42bff9f7"), Boolsk)
    val AntallGVernepliktId = Opplysningstype.Id(UUID.fromString("0194881f-9421-766c-9dc6-41fe6c9a1dff"), Desimaltall)
    val VernepliktGrunnlagId = Opplysningstype.Id(UUID.fromString("0194881f-9421-766c-9dc6-41fe6c9a1e00"), Penger)
    val VernepliktPeriodeId = Opplysningstype.Id(UUID.fromString("0194881f-9421-766c-9dc6-41fe6c9a1e01"), Heltall)
    val VernepliktFastsattVanligArbeidstidId = Opplysningstype.Id(UUID.fromString("0194881f-9421-766c-9dc6-41fe6c9a1e02"), Desimaltall)
    val GrunnlagUtenVernepliktId = Opplysningstype.Id(UUID.fromString("0194881f-9421-766c-9dc6-41fe6c9a1e04"), Penger)
    val GrunnlagHvisVernepliktId = Opplysningstype.Id(UUID.fromString("0194881f-9421-766c-9dc6-41fe6c9a1e03"), Penger)
    val GrunnlagForVernepliktErGunstigstId = Opplysningstype.Id(UUID.fromString("0194881f-9421-766c-9dc6-41fe6c9a1e05"), Boolsk)
    val FagsakIdId = Opplysningstype.Id(UUID.fromString("0194881f-9462-78af-8977-46092bb030eb"), Heltall)
    val avtjentVernepliktId = Opplysningstype.Id(UUID.fromString("01948d3c-4bea-7802-9d18-5342a5e2be99"), Boolsk)
    val oppfyllerKravetTilVernepliktId = Opplysningstype.Id(UUID.fromString("01948d43-e218-76f1-b29b-7e604241d98a"), Boolsk)

    // permittering
    val godkjentPermitteringsårsakId = Opplysningstype.Id(UUID.fromString("0194d105-bd54-7b2b-9dc6-6e6664951894"), Boolsk)
    val oppfyllerKravetTilPermitteringId = Opplysningstype.Id(UUID.fromString("0194d111-db2f-7395-bcfb-959f245fd2a6"), Boolsk)
    val erPermitteringenMidlertidigId = Opplysningstype.Id(UUID.fromString("0194d119-90b7-7416-a8b0-9e9cf3587d48"), Boolsk)
    val permitteringsperiodeId = Opplysningstype.Id(UUID.fromString("0195042d-918e-7fae-8fb7-7f38eed42710"), Heltall)

    // permittering fiskeindustri
    val oppfyllerKravetTilPermitteringFiskeindustriId = Opplysningstype.Id(UUID.fromString("019522b0-c722-76d4-8d7f-78f556c51f72"), Boolsk)
    val godkjentÅrsakPermitteringFraFiskindustriId = Opplysningstype.Id(UUID.fromString("019522b8-0f1b-7536-8691-fd824bca86de"), Boolsk)
    val erPermitteringenFraFiskeindustriMidlertidigId = Opplysningstype.Id(UUID.fromString("019522b8-494f-7012-898c-d202e3b90061"), Boolsk)
    val kravTilProsentvisTapAvArbeidstidFiskepermitteringId =
        Opplysningstype.Id(
            UUID.fromString("019522d2-9bb1-7960-b1e2-a959566e2428"),
            Desimaltall,
        )
    val permitteringFraFiskeindustriPeriodeId = Opplysningstype.Id(UUID.fromString("0195235a-599b-7b27-97a8-bc6142066a87"), Heltall)

    // beregning/meldekort
    val meldeperiodeId = Opplysningstype.Id(UUID.fromString("01956abd-2871-7517-a332-b462c0c31292"), PeriodeDataType)
    val arbeidsdagId = Opplysningstype.Id(UUID.fromString("01948ea0-36e8-72cc-aa4f-16bc446ed3bd"), Boolsk)
    val arbeidstimerId = Opplysningstype.Id(UUID.fromString("01948ea0-e25c-7c47-8429-a05045d80eca"), Desimaltall)
    val forbrukId = Opplysningstype.Id(UUID.fromString("01948ea0-ffdc-7964-ab55-52a7e35e1020"), Boolsk)

    val terskelId = Opplysningstype.Id(UUID.fromString("01948ea2-22f3-7da8-9547-90d0c64e74e0"), Desimaltall)
    val meldtId = Opplysningstype.Id(UUID.fromString("01956ab8-126c-7636-803e-a5d87eda2015"), Boolsk)
    val forbruktEgenandelId = Opplysningstype.Id(UUID.fromString("01973a27-d8b3-7ffd-a81a-a3826963b079"), Heltall)
    val utbetalingId = Opplysningstype.Id(UUID.fromString("01957069-d7d5-7f7c-b359-c00686fbf1f7"), Heltall)
    val utbetalingForPeriodeId = Opplysningstype.Id(UUID.fromString("01994cfd-9a27-762e-81fa-61f550467c95"), Heltall)

    // Hendelser
    val HendelseTypeId = Opplysningstype.Id(UUID.fromString("01958ef2-e237-77c4-89e1-de91256e2e4a"), Tekst)
    val skalUføreSamordnesId = Opplysningstype.Id(UUID.fromString("0196afaf-afbd-7079-b2cf-3669ad9d86aa"), Boolsk)
    val uføreBeløpSomSkalSamordnesId = Opplysningstype.Id(UUID.fromString("0196afbf-e32d-775a-ad10-f476e26dcb6f"), Penger)
    val sumHvisUføreIkkeSkalSamordnesId = Opplysningstype.Id(UUID.fromString("0196afc0-6807-7fa3-83e4-cf7f621f3a7e"), Penger)
    val arbeidstidsreduksjonIkkeBruktTidligereId = Opplysningstype.Id(UUID.fromString("0196b4a7-23b5-7b2c-aa95-e4167d900de8"), Boolsk)

    val harLøpendeRettId = Opplysningstype.Id(UUID.fromString("01990a09-0eab-7957-b88f-14484a50e194"), Boolsk)
    val forbrukteDagerId = Opplysningstype.Id(UUID.fromString("01992934-66e4-7606-bdd3-c6c9dd420ffd"), Heltall)
    val gjenståendeDagerId = Opplysningstype.Id(UUID.fromString("01992956-e349-76b1-8f68-c9d481df3a32"), Heltall)
}

/**
 * Lager ny UUID for en ny opplysningstype
 */
fun main() {
    println(UUIDv7.ny())
}
