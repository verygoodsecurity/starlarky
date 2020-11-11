cp -avr $(readlink -f ../pylarky) artifacts/pylarky
cp  $(readlink -f ../larky/target/larky-runner) artifacts
docker build -t larky_server .