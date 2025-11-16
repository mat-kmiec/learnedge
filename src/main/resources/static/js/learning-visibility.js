document.addEventListener("DOMContentLoaded", () => {
    const userLearningStyle = document.getElementById("userLearningStyle").value; // przykładowo: 1 - słuchowiec, 2 - wzrokowiec, 3 - kinestetyk

    document.querySelectorAll("[data-learning]").forEach(el => {
        const attr = el.getAttribute("data-learning");

        const values = attr.split(",").map(v => v.trim());
        if (values.includes("0") || values.includes(userLearningStyle.toString())) {
            el.style.display = "";
        } else {
            el.style.display = "none";
        }
    });
});