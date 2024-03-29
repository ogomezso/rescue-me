package rescueme.com.app.infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import rescueme.com.app.domain.shelter.{Shelter, ShelterService}

class ShelterEndpoint[F[_]: Sync] extends Http4sDsl[F] {

  implicit val shelterDecoder: EntityDecoder[F, Shelter] = jsonOf

  private def findAllShelters(shelterService: ShelterService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root =>
        for {
          all  <- shelterService.all()
          resp <- Ok(all.asJson)
        } yield resp
    }

  private def createShelter(shelterService: ShelterService[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case req @ POST -> Root =>
        val result = for {
          shelter <- req.as[Shelter]
          res     <- shelterService.create(shelter).value
        } yield res
        result.flatMap {
          case Right(shelterCreated) => Ok(shelterCreated.asJson)
          case Left(_)               => Conflict(s"A shelter with these fields already exists")
        }
    }
  }

  private def get(shelterService: ShelterService[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root / LongVar(id) =>
        shelterService.get(id).value flatMap {
          case Left(_)        => BadRequest(s"Shelter with id: $id not found")
          case Right(shelter) => Ok(shelter.asJson)
        }
    }
  }

  def endpoints(shelterService: ShelterService[F]): HttpRoutes[F] =
    findAllShelters(shelterService) <+>
      createShelter(shelterService) <+>
      get(shelterService)

}
object ShelterEndpoint {
  def endpoints[F[_]: Sync](shelterService: ShelterService[F]): HttpRoutes[F] =
    new ShelterEndpoint[F].endpoints(shelterService)
}
