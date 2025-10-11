package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.tournament.Tournament;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "Events")
@Getter
@Setter
public class Event {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String displayName;

    @Column(nullable = false)
    private String tier;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer numberDays;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private Integer numberPlayers;

    @Column(nullable = false)
    private Integer points;

    @Column
    private Double purse;

    @Column(nullable = false)
    private Boolean isChampionship;

    @Column(nullable = false)
    private Boolean isSwisstour;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Tournament> tournaments = new HashSet<>();

}
