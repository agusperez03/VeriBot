# Frontend Chat UI

A simple, containerized frontend chat interface built with vanilla HTML, CSS, and JavaScript.
It communicates with a backend service specified by the `BACKEND_URL` environment variable.

## Prerequisites

- Docker

## How to Build and Run

1.  **Build the Docker image:**

    ```bash
    docker build -t frontend-chat .
    ```

2.  **Run the Docker container:**

    Replace `<tu-backend>` with the actual URL of your backend service (e.g., `http://localhost:8080` or `http://your-backend-api.com`).

    ```bash
    docker run -e BACKEND_URL=http://<tu-backend> -p 80:80 frontend-chat
    ```

    *   `-e BACKEND_URL=...`: Sets the environment variable for the backend API endpoint.
    *   `-p 80:80`: Maps port 80 from your host machine to port 80 inside the container.

3.  Open your web browser and navigate to `http://localhost` (or your Docker host IP if not running locally).

## Project Structure

```
frontend-chat/
├── index.html       # Main HTML structure
├── styles.css       # CSS styles
├── app.js           # JavaScript for API interaction and UI updates
├── Dockerfile       # Docker configuration
├── entrypoint.sh    # Script to configure and start the server in the container
└── README.md        # This file
```