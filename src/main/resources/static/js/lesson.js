// /js/lesson.js

document.addEventListener("DOMContentLoaded", function () {
    // 1. Spróbuj znaleźć przycisk
    const button = document.getElementById("completeLessonBtn");

    // 2. TYLKO jeśli przycisk istnieje, dodaj logikę
    if (button) {
        const lessonId = button.dataset.lessonId;

        button.addEventListener("click", function () {
            button.disabled = true; // Wyłącz od razu

            fetch(`/api/${lessonId}/complete`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                }
            })
                .then(response => {
                    if (response.ok) {
                        button.innerHTML = '<i class="bi bi-check-circle me-2"></i> Lekcja ukończona';
                    } else {
                        alert("❌ Wystąpił problem przy zapisie lekcji.");
                        button.disabled = false; // Włącz z powrotem przy błędzie
                    }
                })
                .catch(error => {
                    console.error("Błąd:", error);
                    alert("❌ Błąd sieci lub serwera.");
                    button.disabled = false; // Włącz z powrotem przy błędzie
                });
        });
    }
    // Jeśli przycisk nie istnieje na stronie, ten kod jest bezpiecznie pomijany.
});