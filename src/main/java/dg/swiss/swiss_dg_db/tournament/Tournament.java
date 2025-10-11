package dg.swiss.swiss_dg_db.tournament;

import dg.swiss.swiss_dg_db.event.Event;
import dg.swiss.swiss_dg_db.player.Player;
import dg.swiss.swiss_dg_db.round.Round;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "Tournaments")
@Getter
@Setter
public class Tournament {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence"
    )
    private Long id;

    @Column(nullable = false)
    private String division;

    @Column(nullable = false)
    private Integer place;

    @Column
    private Integer rating;

    @Column
    private Double prize;

    @Column
    private Integer score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Round> rounds = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

}
