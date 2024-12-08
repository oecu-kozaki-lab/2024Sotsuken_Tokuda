window.addEventListener('load', () => {
    const endpoint = "https://lod.hozo.jp/kz-fuseki/dis/sparql";

    const sendButton = document.getElementById('send');
    const resultArea = document.getElementById('result_div');
    const symptomArea = document.getElementById('symptom_area');
    const historyStack = []; // 履歴を保存するスタック

    let filteredDiseases = []; // 現在の疾患候補
    let usedSymptoms = []; // 使用された症状

    // 「戻る」ボタンの設定
    const backButton = document.createElement('button');
    backButton.textContent = "戻る";
    backButton.style.display = "none"; // 履歴がない場合は非表示
    backButton.onclick = () => {
        if (historyStack.length > 0) {
            const previousState = historyStack.pop(); // 履歴から状態を取得
            filteredDiseases = previousState.filteredDiseases;
            usedSymptoms = previousState.usedSymptoms;
            updateUI(previousState); // UIを更新

            // 履歴がなくなったら戻るボタンを非表示
            if (historyStack.length === 0) {
                backButton.style.display = "none";
            }
        }
    };
    document.body.appendChild(backButton);

    sendButton.addEventListener('click', async () => {
        const initialSymptom = document.getElementById('INPUT').value;
        usedSymptoms.push(initialSymptom);

        // SPARQLクエリ
        const query = `
        PREFIX dis_p: <https://hozo.jp/dis/prop/> 
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
        SELECT DISTINCT ?s ?sLabel 
        WHERE {
            ?s dis_p:sym ?o .
            ?o rdfs:label "${initialSymptom}"@ja .
            ?s rdfs:label ?sLabel .
        }`;

        // 結果をクリア
        clearElement(resultArea);
        clearElement(symptomArea);

        try {
            const result = await sendQuery(endpoint, query);
            if (!result.ok) {
                resultArea.innerText = "クエリが失敗しました。";
                return;
            }

            const resultData = await result.json();
            filteredDiseases = mergeDiseasesByUri(resultData.results.bindings);

            saveCurrentState(); // 状態を履歴に保存
            updateUI({ filteredDiseases, usedSymptoms });
        } catch (error) {
            resultArea.innerHTML = error.message;
        }
    });

    // 状態を保存する関数
    function saveCurrentState() {
        historyStack.push({
            filteredDiseases: [...filteredDiseases], // 深いコピー
            usedSymptoms: [...usedSymptoms],
        });
        backButton.style.display = "block"; // 戻るボタンを表示
    }

    // UIを更新する関数
    function updateUI(state) {
        displayDiseases(state.filteredDiseases);

        // 症状ボタンを更新
        if (state.filteredDiseases.length === 1) {
            clearElement(symptomArea); // 疾患が1つになったら症状ボタンをクリア
        } else {
            showSymptomsForDiseases(state.filteredDiseases, state.usedSymptoms);
        }
    }

    // 疾患候補を表示する関数
    function displayDiseases(diseases) {
        clearElement(resultArea);

        const diseaseList = document.createElement('ul');
        diseases.forEach(disease => {
            const listItem = document.createElement('li');
            listItem.textContent = disease.label;
            diseaseList.appendChild(listItem);
        });

        resultArea.appendChild(diseaseList);
    }

    // 症状ボタンを表示する関数
    async function showSymptomsForDiseases(diseases, usedSymptoms) {
        const symptomsMap = new Map();

        for (const disease of diseases) {
            const query = `
            PREFIX dis_p: <https://hozo.jp/dis/prop/> 
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
            SELECT DISTINCT ?symptom ?symptomLabel 
            WHERE {
                <${disease.uri}> dis_p:sym ?symptom .
                ?symptom rdfs:label ?symptomLabel .
            }`;

            try {
                const result = await sendQuery(endpoint, query);
                if (!result.ok) continue;

                const resultData = await result.json();
                resultData.results.bindings.forEach(item => {
                    const symptomLabel = item['symptomLabel'].value;
                    if (!usedSymptoms.includes(symptomLabel)) {
                        if (!symptomsMap.has(symptomLabel)) {
                            symptomsMap.set(symptomLabel, []);
                        }
                        symptomsMap.get(symptomLabel).push(disease.label);
                    }
                });
            } catch (error) {
                console.error("症状取得エラー:", error);
            }
        }

        clearElement(symptomArea);

        symptomsMap.forEach((_, symptom) => {
            const button = document.createElement('button');
            button.textContent = symptom;
            button.className = 'symptom-button';
            button.onclick = () => {
                saveCurrentState(); // 状態を保存
                usedSymptoms.push(symptom);

                const nextDiseases = filteredDiseases.filter(disease =>
                    symptomsMap.get(symptom).includes(disease.label)
                );
                filteredDiseases = nextDiseases;

                updateUI({ filteredDiseases, usedSymptoms });
            };
            symptomArea.appendChild(button);
        });
    }

    // 疾患ラベルを統合する関数
    function mergeDiseasesByUri(bindings) {
        const merged = new Map();

        bindings.forEach(item => {
            const uri = item['s'].value;
            const label = item['sLabel'].value;

            if (!merged.has(uri)) {
                merged.set(uri, { uri, labels: [] });
            }
            merged.get(uri).labels.push(label);
        });

        return Array.from(merged.values()).map(disease => ({
            uri: disease.uri,
            label: disease.labels.join(', '),
        }));
    }

    // 要素をクリアする関数
    function clearElement(element) {
        while (element.firstChild) {
            element.removeChild(element.firstChild);
        }
    }

    // SPARQLクエリを送信する関数
    async function sendQuery(endpoint, query) {
        const response = await fetch(`${endpoint}?${new URLSearchParams({ query, output: 'json' })}`);
        return response;
    }
});
