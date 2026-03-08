package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.tournament.Tournament;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Events")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true)
    private Long eventId;

    @Column private String name;

    @Column private String displayName;

    @Column private String tier;

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column private String city;

    @Column private String country;

    @Column private Integer numberPlayers;

    @Column(nullable = false)
    private Integer points;

    @Column private Double purse;

    @Column(nullable = false)
    private Boolean isChampionship;

    @Column(nullable = false)
    private Boolean isSwisstour;

    @Builder.Default
    @Column(nullable = false)
    private Boolean hasResults = false;

    @Column private String infoLink;

    @Column private String registrationLink;

    @Column private LocalDate registrationStart;

    @Column private String swisstourType;

    @Column private LocalDate startDate;

    @Column private LocalDate endDate;

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Tournament> tournaments = new HashSet<>();
}
