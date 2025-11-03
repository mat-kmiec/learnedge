document.addEventListener("DOMContentLoaded", () => {
    const userLearningStyle = 3; // przykładowo: 1 - słuchowiec, 2 - wzrokowiec, 3 - kinestetyk

    document.querySelectorAll("[data-learning]").forEach(el => {
        const attr = el.getAttribute("data-learning");

        // jeśli 0 lub zawiera styl użytkownika → pokazuj
        const values = attr.split(",").map(v => v.trim());
        if (values.includes("0") || values.includes(userLearningStyle.toString())) {
            el.style.display = "";
        } else {
            el.style.display = "none";
        }
    });
});