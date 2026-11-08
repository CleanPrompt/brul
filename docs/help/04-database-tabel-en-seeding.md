# Stap 4: Database Tabel & Schema Initialisatie

In deze stap hebben we de database-tabel **`HelpParticipationsTable`** gedefinieerd met behulp van **JetBrains Exposed 0.61 DSL** en geregistreerd in de centrale **`DatabaseFactory`**.

---

## 1. Wat is Exposed DSL en waarom gebruiken we dit?

Veel studenten kennen frameworks zoals Spring Data JPA of Hibernate. Exposed is JetBrains' officiële SQL-framework voor Kotlin. Exposed kent twee varianten:
* **DAO (Data Access Object):** Object-georiënteerd, gebruikt entiteitsklassen met lazy loading (vaak complex en verborgen queries).
* **DSL (Domain-Specific Language):** Type-veilige SQL-bouwer in pure Kotlin.

### Waarom eist het Brul-framework Exposed DSL?
1. **Geen verborgen queries:** Elke query (`select`, `insert`, `join`) is expliciet zichtbaar in code. Er zijn geen verborgen "N+1 query" valkuilen.
2. **KISS & Functioneel:** Tabellen zijn eenvoudige Kotlin `object` singletons. Rijen worden via pure functies gemapped (`ResultRow.toHelpParticipation()`).
3. **Compile-time SQL validatie:** Typ je een typfout in een kolomnaam of tabel? Dan weigert de Kotlin-compiler direct te bouwen.

---

## 2. De Tabel Definitie (`HelpParticipationsTable.kt`)

```kotlin
package nl.brul.backend.features.help.database

import nl.brul.backend.features.help.model.ParticipationStatus
import nl.brul.backend.features.user.database.UsersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object HelpParticipationsTable : Table("help_participations") {
    val id = uuid("id")
    val helpRequestId = uuid("help_request_id")
    
    /** Foreign key: verwijst naar UsersTable.id */
    val userId = reference("user_id", UsersTable.id)
    
    /** Slaat de enum op als leesbare string ("SIGNED_UP", "ATTENDED") */
    val status = enumerationByName("status", 30, ParticipationStatus::class)
    
    /** UTC tijdstempel van de check-in, of null als er nog niet is ingecheckt */
    val checkedInAt = timestamp("checked_in_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        // Database-constraint tegen dubbele aanmeldingen
        uniqueIndex("uq_help_participation_request_user", helpRequestId, userId)
    }
}
```

### Belangrijke concepten voor het examen:

#### A. `reference("user_id", UsersTable.id)` (Foreign Key)
* Dit creëert een echte SQL Foreign Key constraint in de database.
* Het garandeert **referentiële integriteit**: je kunt nooit een inschrijving opslaan voor een gebruiker die niet bestaat in `UsersTable`.

#### B. `enumerationByName("status", 30, ParticipationStatus::class)`
* In plaats van een getal (0, 1, 2) slaat Exposed de enum op als tekst (`"SIGNED_UP"`, `"ATTENDED"`).
* Voordeel: Als je de database inspecteert (bijv. in DBeaver of de H2 console), is de data direct begrijpelijk voor mensen. Verander je ooit de volgorde van de enum in code, dan raakt de database niet corrupt!

#### C. `uniqueIndex("uq_help_participation_request_user", helpRequestId, userId)`
* **Dé bescherming tegen race conditions:** Stel dat een vrijwilliger heel snel twee keer achter elkaar op "Aanmelden" klikt, of twee requests tegelijk binnenkomen.
* Zelfs als beide requests tegelijk door de servicelaag glippen, weigert de database-engine de tweede INSERT met een SQL State 23505 (Unique index violation). Dit voorkomt dat een vrijwilliger dubbel geregistreerd raakt.

---

## 3. De Database Mapper (`HelpParticipationDatabaseMapper.kt`)

Om een database-resultaat om te zetten naar ons schone domeinmodel, gebruiken we een extension function op Exposed's `ResultRow`:

```kotlin
fun ResultRow.toHelpParticipation(): HelpParticipation =
    HelpParticipation(
        id = this[HelpParticipationsTable.id],
        helpRequestId = this[HelpParticipationsTable.helpRequestId],
        userId = this[HelpParticipationsTable.userId],
        status = this[HelpParticipationsTable.status],
        checkedInAt = this[HelpParticipationsTable.checkedInAt]
    )
```

Dit houdt de database-laag 100% geïsoleerd. De servicelaag heeft hierdoor geen weet van Exposed of SQL `ResultRow` objecten.

---

## 4. Schema Initialisatie in `DatabaseFactory.kt`

In `backend/src/config/DatabaseFactory.kt` is de tabel toegevoegd aan de opstartprocedure:

```kotlin
transaction {
    SchemaUtils.create(
        UsersTable,
        RolesTable,
        PermissionsTable,
        UserRolesTable,
        RolePermissionsTable,
        FeedbackTable,
        HelpParticipationsTable  // <-- Onze nieuwe tabel
    )
}
```

### Hoe werkt `SchemaUtils.create`?
* `SchemaUtils.create` voert een `CREATE TABLE IF NOT EXISTS` uit.
* Als de tabel al bestaat, doet hij niets.
* Omdat dit een **gloednieuwe tabel** is, hoeven we de bestaande database (`data/brul*`) **niet** te wissen of opnieuw te initialiseren!
