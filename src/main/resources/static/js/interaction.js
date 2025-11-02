document.addEventListener("click", (e) => {
    if (e.target.classList.contains("course-quiz-option")) {
        const btn = e.target;
        const quizBlock = btn.closest(".course-quiz-block");
        const feedback = quizBlock.querySelector(".course-quiz-feedback");
        const isCorrect = btn.dataset.correct === "true";

        quizBlock.querySelectorAll(".course-quiz-option").forEach((b) => {
            b.disabled = true;
            if (b.dataset.correct === "true") b.classList.add("correct");
        });

        if (isCorrect) {
            btn.classList.add("correct");
            feedback.innerHTML = `<span class="text-success fw-bold"><i class="bi bi-check-circle"></i> Dobrze! To poprawna odpowiedź.</span>`;
        } else {
            btn.classList.add("incorrect");
            feedback.innerHTML = `<span class="text-danger fw-bold"><i class="bi bi-x-circle"></i> Błędna odpowiedź!</span>`;
        }
    }
});

document.addEventListener("click", (e) => {
    if (e.target.classList.contains("check-answer-btn")) {
        const quizBlock = e.target.closest(".course-quiz-block");
        const input = quizBlock.querySelector(".course-answer-input");
        const feedback = quizBlock.querySelector(".course-quiz-feedback");

        const userAnswer = input.value.trim().toLowerCase();
        const correctAnswer = input.dataset.answer.toLowerCase();

        if (!userAnswer) {
            feedback.innerHTML = `<span class="text-warning">⚠️ Wpisz odpowiedź przed sprawdzeniem.</span>`;
            return;
        }

        if (userAnswer === correctAnswer) {
            feedback.innerHTML = `<span class="text-success fw-bold"><i class="bi bi-check-circle"></i> Dobrze! Odpowiedź poprawna.</span>`;
        } else {
            feedback.innerHTML = `<span class="text-danger fw-bold"><i class="bi bi-x-circle"></i> Błędnie! Poprawna odpowiedź to: <span class="text-info">${input.dataset.answer}</span></span>`;
        }
    }
});

document.addEventListener("click", (e) => {
    const btn = e.target.closest(".course-copy-btn");
    if (!btn) return;

    const codeBlock = btn.closest(".course-code-block");
    if (!codeBlock) return;

    const code = codeBlock.querySelector("code").innerText;
    navigator.clipboard.writeText(code);

    btn.innerHTML = '<i class="bi bi-check-circle"></i> Skopiowano!';
    btn.classList.add("copied");

    setTimeout(() => {
        btn.innerHTML = '<i class="bi bi-clipboard"></i> Kopiuj';
        btn.classList.remove("copied");
    }, 2000);
});

document.addEventListener("click", (e) => {
    if (e.target.classList.contains("check-practice-btn")) {
        const block = e.target.closest(".course-practice-block");
        const input = block.querySelector(".practice-input");
        const feedback = block.querySelector(".course-quiz-feedback");

        const userAnswer = input.value.trim().toLowerCase();
        const correct = input.dataset.answer.trim().toLowerCase();

        if (!userAnswer) {
            feedback.innerHTML = `<span class="text-warning">⚠️ Wpisz odpowiedź przed sprawdzeniem.</span>`;
            return;
        }

        if (userAnswer === correct) {
            feedback.innerHTML = `<span class="text-success fw-bold"><i class="bi bi-check-circle"></i> Dobrze! Odpowiedź poprawna.</span>`;
            input.classList.add("correct");
        } else {
            feedback.innerHTML = `<span class="text-danger fw-bold"><i class="bi bi-x-circle"></i> Błąd! Poprawna odpowiedź to: <span class="text-info">${input.dataset.answer}</span></span>`;
            input.classList.add("incorrect");
        }
    }
});