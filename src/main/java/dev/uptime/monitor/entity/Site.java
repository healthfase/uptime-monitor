
package dev.uptime.monitor.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    private String name;

    public Site() {
    }

    public Site(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }
}
