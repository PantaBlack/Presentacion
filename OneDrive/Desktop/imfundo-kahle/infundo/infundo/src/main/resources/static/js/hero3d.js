/**
 * Escena 3D del hero: la camara vuela en bucle continuo por cuatro
 * "estaciones" flotantes (formas de bajo poligono + satelites orbitando),
 * como una vitrina que representa idiomas, clases, comunidad y logros.
 *
 * Progressive enhancement puro: si Three.js no cargo, no hay WebGL,
 * o el usuario pidio "prefers-reduced-motion", esta funcion no hace
 * nada y el hero se queda con su imagen estatica de siempre (ver CSS
 * ".hero-3d-on" en style.css).
 */
(function () {
    'use strict';

    var heroEl = document.querySelector('.hero');
    var container = document.getElementById('hero3d');
    var canvas = document.getElementById('hero3dCanvas');
    if (!heroEl || !container || !canvas || typeof THREE === 'undefined') {
        return;
    }

    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        return;
    }

    var renderer, scene, camera, path, stars;
    var groups = [];

    try {
        renderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: true, alpha: true, powerPreference: 'low-power' });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
        renderer.setClearColor(0x000000, 0);

        scene = new THREE.Scene();
        scene.fog = new THREE.FogExp2(0xeaf1fb, 0.032);

        camera = new THREE.PerspectiveCamera(58, 1, 0.1, 200);

        // ---- Luces: navy + naranja de marca, para que las formas se sientan "Viva Idiomas" ----
        scene.add(new THREE.AmbientLight(0xffffff, 0.55));
        var keyLight = new THREE.PointLight(0xf15a24, 2.4, 60);
        keyLight.position.set(2, 6, 2);
        scene.add(keyLight);
        var rimLight = new THREE.PointLight(0x1e3a6e, 2.4, 60);
        rimLight.position.set(-2, -6, -2);
        scene.add(rimLight);

        // ---- Particulas de fondo, para dar sensacion de profundidad al volar ----
        var starCount = 240;
        var starGeo = new THREE.BufferGeometry();
        var starPos = new Float32Array(starCount * 3);
        for (var i = 0; i < starCount; i++) {
            starPos[i * 3] = (Math.random() - 0.5) * 90;
            starPos[i * 3 + 1] = (Math.random() - 0.5) * 50;
            starPos[i * 3 + 2] = (Math.random() - 0.5) * 90;
        }
        starGeo.setAttribute('position', new THREE.BufferAttribute(starPos, 3));
        stars = new THREE.Points(starGeo, new THREE.PointsMaterial({
            color: 0x1e3a6e, size: 0.16, transparent: true, opacity: 0.35
        }));
        scene.add(stars);

        // ---- Ruta de vuelo: curva cerrada que pasa cerca de cada estacion ----
        var waypoints = [
            new THREE.Vector3(0, 0.5, 15),
            new THREE.Vector3(10, 2, 3),
            new THREE.Vector3(5, -1.5, -10),
            new THREE.Vector3(-9, 2.5, -5),
            new THREE.Vector3(-8, -1, 8)
        ];
        path = new THREE.CatmullRomCurve3(waypoints, true, 'catmullrom', 0.5);

        // ---- Estaciones: cada una es un nucleo + su malla wireframe + satelites orbitando ----
        var palette = [0x1e3a6e, 0xf15a24, 0x2c5cc5, 0xfdb44b];
        var stationCenters = [
            new THREE.Vector3(6, 1, 6),
            new THREE.Vector3(8, -2, -6),
            new THREE.Vector3(-4, 2, -9),
            new THREE.Vector3(-8, -1.5, 5)
        ];
        var geometries = [
            function () { return new THREE.IcosahedronGeometry(1.1, 0); },
            function () { return new THREE.TorusKnotGeometry(0.8, 0.26, 90, 12); },
            function () { return new THREE.OctahedronGeometry(1.15, 0); },
            function () { return new THREE.DodecahedronGeometry(1, 0); }
        ];

        stationCenters.forEach(function (center, idx) {
            var group = new THREE.Group();
            group.position.copy(center);

            var color = palette[idx % palette.length];
            var core = new THREE.Mesh(
                geometries[idx % geometries.length](),
                new THREE.MeshStandardMaterial({
                    color: color, roughness: 0.28, metalness: 0.35,
                    emissive: color, emissiveIntensity: 0.18
                })
            );
            group.add(core);

            var wire = new THREE.Mesh(
                geometries[(idx + 1) % geometries.length](),
                new THREE.MeshBasicMaterial({ color: 0xffffff, wireframe: true, transparent: true, opacity: 0.18 })
            );
            wire.scale.setScalar(1.9);
            group.add(wire);

            var satellites = [];
            for (var s = 0; s < 4; s++) {
                var satColor = palette[(idx + s + 1) % palette.length];
                var sat = new THREE.Mesh(
                    new THREE.SphereGeometry(0.16, 12, 12),
                    new THREE.MeshStandardMaterial({ color: satColor, emissive: satColor, emissiveIntensity: 0.6 })
                );
                sat.userData = {
                    radius: 2.4 + s * 0.35,
                    speed: 0.4 + s * 0.15,
                    offset: s * 1.4,
                    height: (s % 2 === 0 ? 1 : -1) * 0.6
                };
                group.add(sat);
                satellites.push(sat);
            }

            scene.add(group);
            groups.push({ group: group, wire: wire, satellites: satellites, spin: 0.12 + idx * 0.05 });
        });
    } catch (err) {
        // WebGL no disponible, contexto perdido, driver raro, etc: nos quedamos con el hero estatico.
        console.warn('hero3d: no se pudo iniciar la escena 3D, se mantiene el hero estatico.', err);
        return;
    }

    // ---- Vuelo continuo de la camara + paralaje suave con el mouse ----
    var duration = 46000; // ms por vuelta completa
    var startTime = null;
    var lastT = 0;
    var mouseX = 0, mouseY = 0, targetMouseX = 0, targetMouseY = 0;
    var running = false;
    var rafId = null;

    window.addEventListener('mousemove', function (e) {
        targetMouseX = (e.clientX / window.innerWidth) - 0.5;
        targetMouseY = (e.clientY / window.innerHeight) - 0.5;
    }, { passive: true });

    function resize() {
        var w = container.clientWidth || heroEl.clientWidth;
        var h = container.clientHeight || heroEl.clientHeight;
        if (!w || !h) { return; }
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
        renderer.setSize(w, h, false);
    }
    resize();
    window.addEventListener('resize', resize);

    function animate(now) {
        if (!running) { return; }
        rafId = requestAnimationFrame(animate);

        if (!startTime) { startTime = now; }
        var dt = Math.min((now - lastT) / 1000, 0.05) || 0.016;
        lastT = now;

        var t = ((now - startTime) % duration) / duration;
        var lookAheadT = (t + 0.012) % 1;
        var pos = path.getPointAt(t);
        var lookAt = path.getPointAt(lookAheadT);

        mouseX += (targetMouseX - mouseX) * 0.04;
        mouseY += (targetMouseY - mouseY) * 0.04;

        camera.position.copy(pos);
        var lookTarget = lookAt.clone();
        lookTarget.x += mouseX * 2.2;
        lookTarget.y -= mouseY * 1.4;
        camera.lookAt(lookTarget);

        groups.forEach(function (g) {
            g.group.rotation.y += g.spin * dt;
            g.group.rotation.x += g.spin * 0.3 * dt;
            g.wire.rotation.y -= g.spin * 1.6 * dt;
            g.satellites.forEach(function (sat) {
                var a = now * 0.0006 * sat.userData.speed + sat.userData.offset;
                sat.position.set(
                    Math.cos(a) * sat.userData.radius,
                    sat.userData.height + Math.sin(a * 1.4) * 0.3,
                    Math.sin(a) * sat.userData.radius
                );
            });
        });

        stars.rotation.y += 0.006 * dt;

        renderer.render(scene, camera);
    }

    function start() {
        if (running) { return; }
        running = true;
        lastT = performance.now();
        rafId = requestAnimationFrame(animate);
    }
    function stop() {
        running = false;
        if (rafId) { cancelAnimationFrame(rafId); rafId = null; }
    }

    // Pausar cuando la pestana esta oculta o el hero sale de pantalla: ahorra bateria/CPU
    // en el resto de la pagina (estadisticas, testimonios, precios...) sin tocar nada de eso.
    var heroVisible = true;
    document.addEventListener('visibilitychange', function () {
        if (document.hidden) { stop(); } else if (heroVisible) { start(); }
    });

    if ('IntersectionObserver' in window) {
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                heroVisible = entry.isIntersecting;
                if (heroVisible && !document.hidden) { start(); } else { stop(); }
            });
        }, { threshold: 0.05 });
        io.observe(heroEl);
    }

    heroEl.classList.add('hero-3d-on');
    start();
})();
