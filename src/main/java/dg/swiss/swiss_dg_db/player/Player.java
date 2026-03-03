package dg.swiss.swiss_dg_db.player;

import dg.swiss.swiss_dg_db.tournament.Tournament;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

import lombok.*;


@Entity
@Table(name = "Players")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Player {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "primary_sequence",
            sequenceName = "primary_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence"
    )
    private Long id;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Column(unique = true)
    private Long pdgaNumber;

    @Column(unique = true)
    private Long sdaNumber;

    @Column(nullable = false)
    private Boolean swisstourLicense;

    @Column(nullable = false)
    private Boolean isPro;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Tournament> tournaments = new HashSet<>();
}
