use axum::{
    routing::get,
    Router,
};
use tokio::net::TcpListener;

#[tokio::main]
async fn main() {
    // Inicializa tracing para logs
    tracing_subscriber::fmt::init();

    // Construye la app con una ruta
    let app = Router::new()
        .route("/", get(root));

    // Ejecuta el servidor en el puerto 3000
    let listener = TcpListener::bind("0.0.0.0:3000").await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

// Handler básico que responde con un string estático
async fn root() -> &'static str {
    "¡Hola, Mundo!"
}